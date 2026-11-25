package com.sharif899.kyc.web.dto;

/**
 * Body for the verify and reject endpoints. A reason is required for
 * rejection (enforced in the service) and optional for verification.
 */
public record DecisionRequest(String reason) {
}
