package com.sharif899.kyc.web.dto;

import java.util.Map;

/**
 * The single response envelope for every endpoint. Success responses carry
 * {@code data}; error responses carry {@code error} and a null {@code data}.
 */
public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(ErrorDetail error) {
        return new ApiResponse<>(false, null, error);
    }

    public record ErrorDetail(String code, String message, Map<String, String> fieldErrors) {

        public ErrorDetail(String code, String message) {
            this(code, message, Map.of());
        }
    }
}
