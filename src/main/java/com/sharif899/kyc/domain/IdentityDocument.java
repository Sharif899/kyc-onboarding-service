package com.sharif899.kyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

/**
 * The identity document presented by the applicant. A value object embedded
 * in the onboarding application. The document number is stored in full in
 * the database (as a real KYC store must, for audit), but it is never
 * returned in full by the API — see {@link com.sharif899.kyc.service.DocumentMasking}.
 */
@Embeddable
public class IdentityDocument {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType documentType;

    @Column(nullable = false, length = 40)
    private String documentNumber;

    @Column(nullable = false, length = 2)
    private String issuingCountry;

    protected IdentityDocument() {
    }

    public IdentityDocument(DocumentType documentType, String documentNumber, String issuingCountry) {
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.issuingCountry = issuingCountry;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getIssuingCountry() {
        return issuingCountry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityDocument other)) return false;
        return documentType == other.documentType
                && Objects.equals(documentNumber, other.documentNumber)
                && Objects.equals(issuingCountry, other.issuingCountry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentType, documentNumber, issuingCountry);
    }
}
