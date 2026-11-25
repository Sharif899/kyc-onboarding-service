package com.sharif899.kyc.web;

import com.sharif899.kyc.domain.ApplicationStatus;
import com.sharif899.kyc.domain.OnboardingApplication;
import com.sharif899.kyc.service.OnboardingService;
import com.sharif899.kyc.web.dto.ApiResponse;
import com.sharif899.kyc.web.dto.ApplicationResponse;
import com.sharif899.kyc.web.dto.CreateApplicationRequest;
import com.sharif899.kyc.web.dto.DecisionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Customer onboarding, versioned at {@code /api/v1/onboarding}.
 *
 * <p>Idempotency: send an {@code Idempotency-Key} header with POST and a
 * retried request returns the original application with HTTP 200 and an
 * {@code X-Idempotency-Replayed: true} header instead of creating a
 * second application.
 */
@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

    private final OnboardingService service;

    public OnboardingController(OnboardingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> create(
            @Valid @RequestBody CreateApplicationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        OnboardingService.CreateOutcome outcome = service.create(request, idempotencyKey);
        HttpStatus status = outcome.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .header("X-Idempotency-Replayed", String.valueOf(outcome.replayed()))
                .body(ApiResponse.ok(ApplicationResponse.from(outcome.application())));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(ApplicationResponse.from(service.getById(id)));
    }

    @GetMapping
    public ApiResponse<List<ApplicationResponse>> list(
            @RequestParam(required = false) ApplicationStatus status) {
        List<OnboardingApplication> applications = status == null
                ? service.listAll()
                : service.listByStatus(status);
        return ApiResponse.ok(applications.stream().map(ApplicationResponse::from).toList());
    }

    @PostMapping("/{id}/verify")
    public ApiResponse<ApplicationResponse> verify(
            @PathVariable UUID id,
            @RequestBody(required = false) DecisionRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(ApplicationResponse.from(service.verify(id, reason)));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ApplicationResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.ok(ApplicationResponse.from(service.reject(id, request.reason())));
    }
}
