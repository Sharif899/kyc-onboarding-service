# kyc-onboarding-service

**Identity verification and KYC onboarding — Spring Boot 3, Java 21, JPA/H2. A customer submits an identity document, the service validates it, checks for duplicates, runs verification, and records every decision in an audit trail.**

> This is the work I do as a Senior Software Engineer at Seamfix: backend services for identity verification, KYC and digital onboarding. This service is that workflow, in code — small enough to read in an afternoon, complete enough to run end to end.

## What it does

| Area | Behaviour |
|---|---|
| **Identity-data validation** | Document-number format per type (NIN = 11 digits, passport = letter + 8 digits, driver's licence = 3 letters + 7 digits), applicant must be 18+, date of birth must be in the past, phone and email format checked. |
| **Duplicate detection** | One document number, one application. A second submission returns **409** with the existing application's id instead of creating a duplicate. |
| **Idempotency** | POST honours an `Idempotency-Key` header — a retried request returns the original application (**200**, `X-Idempotency-Replayed: true`) instead of a second application. |
| **Verification** | A `VerificationProvider` interface decides; a deterministic mock stands in for a real provider (document number ending in an even digit verifies, odd goes to manual review). Swap in a real provider by implementing the interface. |
| **State machine** | `PENDING → UNDER_REVIEW → VERIFIED / REJECTED / NEEDS_MORE_INFO`. `VERIFIED` and `REJECTED` are terminal — enforced in one place, so no endpoint can bypass it. |
| **Audit trail** | Every state change is recorded (from → to, actor, reason, timestamp) and returned with the application. |
| **Privacy** | Document numbers are stored for audit but **never returned or logged in full** — API responses and logs only ever show the masked form (`*******8901`). |

## Quick start

Requires Java 17+ and Maven (or use the included wrapper).

```bash
git clone https://github.com/Sharif899/kyc-onboarding-service.git
cd kyc-onboarding-service
./mvnw spring-boot:run
```

The service runs on `http://localhost:8080` with an in-memory H2 database — no external database needed. All data is synthetic test data; there is no real personal data anywhere in this repo.

## API

All endpoints live under `/api/v1/onboarding` and return one envelope:

```json
{ "success": true, "data": { "..." }, "error": null }
```

Errors use the same envelope: `{ "success": false, "data": null, "error": { "code": "...", "message": "...", "fieldErrors": {} } }`.

### Create an application — `POST /api/v1/onboarding`

```bash
curl -X POST http://localhost:8080/api/v1/onboarding \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: onboarding-001' \
  -d '{
    "fullName": "Adaeze Okonkwo",
    "dateOfBirth": "1994-03-14",
    "phone": "+2348066661895",
    "email": "adaeze@example.com",
    "documentType": "NIN",
    "documentNumber": "12345678902",
    "issuingCountry": "NG"
  }'
```

Response — `201 Created` (note the masked document number and the creation audit entry):

```json
{
  "success": true,
  "data": {
    "id": "3f2a1b0c-...",
    "fullName": "Adaeze Okonkwo",
    "dateOfBirth": "1994-03-14",
    "phone": "+2348066661895",
    "email": "adaeze@example.com",
    "documentType": "NIN",
    "maskedDocumentNumber": "*******8902",
    "issuingCountry": "NG",
    "status": "PENDING",
    "verificationReason": null,
    "rejectionReason": null,
    "auditTrail": [
      { "fromStatus": null, "toStatus": "PENDING", "actor": "SYSTEM",
        "reason": "Application created", "timestamp": "2026-11-25T10:00:00" }
    ],
    "createdAt": "2026-11-25T10:00:00",
    "updatedAt": "2026-11-25T10:00:00"
  },
  "error": null
}
```

### Fetch one — `GET /api/v1/onboarding/{id}`

```bash
curl http://localhost:8080/api/v1/onboarding/3f2a1b0c-...
```

### List / filter — `GET /api/v1/onboarding?status=PENDING`

```bash
curl "http://localhost:8080/api/v1/onboarding?status=PENDING"
```

### Verify — `POST /api/v1/onboarding/{id}/verify`

```bash
curl -X POST http://localhost:8080/api/v1/onboarding/3f2a1b0c-.../verify \
  -H 'Content-Type: application/json' -d '{}'
```

The mock provider verifies document numbers ending in an **even** digit and refers odd-ending numbers to manual review (`UNDER_REVIEW`). The response carries the provider's reason and the new audit entry. Re-running verification on an application already under review is meaningful rather than an error: a second inconclusive check moves it to `NEEDS_MORE_INFO` (ask the applicant for more information), and verifying from `NEEDS_MORE_INFO` sends it back to `UNDER_REVIEW` once more information has been supplied.

### Reject — `POST /api/v1/onboarding/{id}/reject`

```bash
curl -X POST http://localhost:8080/api/v1/onboarding/3f2a1b0c-.../reject \
  -H 'Content-Type: application/json' \
  -d '{ "reason": "Document photo is unreadable" }'
```

A reason is required — a rejection without one is a `400`.

### Duplicate and idempotency, in one picture

```bash
# Same document number again → 409, with the existing application's id
curl -X POST http://localhost:8080/api/v1/onboarding -H 'Content-Type: application/json' -d '{ ...same NIN... }'
# { "success": false, "error": { "code": "DUPLICATE_APPLICATION",
#   "message": "An application already exists for this document number",
#   "fieldErrors": { "existingApplicationId": "3f2a1b0c-..." } } }

# Same Idempotency-Key again → 200, the original application, no second row
curl -X POST http://localhost:8080/api/v1/onboarding \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: onboarding-001' -d '{ ... }'
# HTTP 200, header X-Idempotency-Replayed: true
```

## How this maps to real KYC work

| Real onboarding task | Here |
|---|---|
| Identity-data validation before any provider call | `DocumentNumberValidator` + Bean Validation on the request; age and date rules in `OnboardingService` |
| Duplicate records across retries and resubmissions | Unique constraint on the document number + `DuplicateApplicationException` (409); idempotency keys on POST |
| Idempotent request handling on flaky networks | `Idempotency-Key` header, stored on the application, replay returns the original |
| Verification against an issuing authority | `VerificationProvider` interface; `MockVerificationProvider` stands in with documented deterministic rules |
| Verification failures traced across logs and database | `AuditLogEntry` for every transition; the state machine is enforced in exactly one place |
| Handling sensitive personal data securely | Document numbers masked in every response and log line; no secrets in the repo |
| Reproducible builds and tests | GitHub Actions CI: `mvn test` then `mvn package` on every push/PR to main |

## Architecture

```
src/main/java/com/sharif899/kyc/
├── KycOnboardingApplication.java   # Spring Boot entry point
├── domain/                          # OnboardingApplication (aggregate root),
│                                    # Customer + IdentityDocument (value objects),
│                                    # AuditLogEntry, ApplicationStateMachine
├── repository/                      # Spring Data JPA repositories
├── service/                         # OnboardingService (workflow rules),
│                                    # VerificationProvider + MockVerificationProvider,
│                                    # DocumentNumberValidator, DocumentMasking
└── web/                             # REST controller, DTOs, global exception handler
```

The state machine is the design's centre: `transitionTo()` is the only way an application's status changes, and it both enforces the allowed transitions and writes the audit entry. That is the same shape as the transaction-state validation used on the KYC services this is modelled on.

## Tests

```bash
./mvnw test
```

49 tests: document-format validation, masking, the state machine, service rules (duplicate, idempotency, age, verification outcomes, re-verification flow, invalid transitions, audit trail), and MockMvc integration tests for every endpoint (happy path, duplicate, idempotency, invalid document, invalid state transition, missing application).

## Limitations — stated honestly

- **In-memory H2 database.** Data disappears when the service stops. For production, point `spring.datasource.url` at PostgreSQL and use `ddl-auto=validate` with Flyway migrations.
- **Mock verification provider.** The even/odd-digit rule is a stand-in so the flow can be exercised without a live identity registry. It is not a real identity check — a real provider implements `VerificationProvider`.
- **Synthetic data only.** Every name, number and email in the tests and examples is invented. No real personal data is in this repository.
- **No authentication yet.** In production this API sits behind the platform's auth (the same way the onboarding services it models do); adding Spring Security is the natural next step, deliberately left out so the workflow itself stays readable.

## Licence

MIT — see [LICENSE](LICENSE).
