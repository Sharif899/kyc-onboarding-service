package com.sharif899.kyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The applicant. A value object embedded in the onboarding application —
 * a customer has no identity outside their application in this service.
 */
@Embeddable
public class Customer {

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 150)
    private String email;

    protected Customer() {
    }

    public Customer(String fullName, LocalDate dateOfBirth, String phone, String email) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;
        return Objects.equals(fullName, other.fullName)
                && Objects.equals(dateOfBirth, other.dateOfBirth)
                && Objects.equals(phone, other.phone)
                && Objects.equals(email, other.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, dateOfBirth, phone, email);
    }
}
