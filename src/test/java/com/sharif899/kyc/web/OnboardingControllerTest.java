package com.sharif899.kyc.web;

import com.sharif899.kyc.repository.OnboardingApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP tests for /api/v1/onboarding. Each test runs in a
 * transaction that is rolled back, so document numbers can be reused
 * across tests without triggering duplicate detection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OnboardingApplicationRepository repository;

    private static String createJson(String documentNumber) {
        return createJson(documentNumber, "NIN");
    }

    private static String createJson(String documentNumber, String documentType) {
        return """
                {
                  "fullName": "Adaeze Okonkwo",
                  "dateOfBirth": "1994-03-14",
                  "phone": "+2348066661895",
                  "email": "adaeze@example.com",
                  "documentType": "%s",
                  "documentNumber": "%s",
                  "issuingCountry": "NG"
                }
                """.formatted(documentType, documentNumber);
    }

    @Test
    void createReturns201WithMaskedDocumentNumber() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678901")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.maskedDocumentNumber", is("*******8901")))
                .andExpect(jsonPath("$.data.documentNumber").doesNotExist())
                .andExpect(jsonPath("$.data.auditTrail", hasSize(1)));
    }

    @Test
    void createWithInvalidEmailReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678901").replace(
                                "adaeze@example.com", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.fieldErrors.email").exists());
    }

    @Test
    void createWithInvalidDocumentFormatReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    @Test
    void createUnderageApplicantReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678901").replace(
                                "1994-03-14", "2015-03-14")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    @Test
    void duplicateDocumentReturns409WithExistingId() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678901")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678901")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_APPLICATION")))
                .andExpect(jsonPath("$.error.fieldErrors.existingApplicationId").exists());
    }

    @Test
    void idempotentReplayReturns200WithSameIdAndNoSecondApplication() throws Exception {
        String first = mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-abc")
                        .content(createJson("12345678901")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-abc")
                        .content(createJson("12345678902")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.data.id", is(first.split("\"id\":\"")[1].split("\"")[0])));

        assertEquals(1, repository.count());
    }

    @Test
    void verifyMovesEvenDocumentToVerified() throws Exception {
        String body = mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678902")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = body.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/v1/onboarding/" + id + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("VERIFIED")))
                .andExpect(jsonPath("$.data.auditTrail", hasSize(2)));
    }

    @Test
    void verifyOnVerifiedApplicationReturns409() throws Exception {
        String body = mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678902")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = body.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/v1/onboarding/" + id + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/onboarding/" + id + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("INVALID_STATE")));
    }

    @Test
    void rejectWithReasonMovesToRejected() throws Exception {
        String body = mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678901")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = body.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/v1/onboarding/" + id + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Document photo is unreadable\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("REJECTED")))
                .andExpect(jsonPath("$.data.rejectionReason", is("Document photo is unreadable")));
    }

    @Test
    void rejectWithoutReasonReturns400() throws Exception {
        String body = mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678901")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = body.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/v1/onboarding/" + id + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    @Test
    void getUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("NOT_FOUND")));
    }

    @Test
    void listFiltersByStatus() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678901")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("12345678902")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        mockMvc.perform(get("/api/v1/onboarding").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        mockMvc.perform(get("/api/v1/onboarding").param("status", "VERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
