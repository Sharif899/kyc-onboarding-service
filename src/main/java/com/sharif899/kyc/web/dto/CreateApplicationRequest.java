package com.sharif899.kyc.web.dto;

import com.sharif899.kyc.domain.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Validated intake for a new onboarding application. */
public record CreateApplicationRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$",
                message = "Phone number must be 10-15 digits, optionally starting with +")
        String phone,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Document number is required")
        String documentNumber,

        @NotBlank(message = "Issuing country is required")
        @Pattern(regexp = "^[A-Z]{2}$",
                message = "Issuing country must be a 2-letter ISO country code (e.g. NG)")
        String issuingCountry
) {
}
