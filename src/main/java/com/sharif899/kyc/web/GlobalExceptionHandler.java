package com.sharif899.kyc.web;

import com.sharif899.kyc.exception.ApplicationNotFoundException;
import com.sharif899.kyc.exception.DuplicateApplicationException;
import com.sharif899.kyc.exception.InvalidDocumentException;
import com.sharif899.kyc.exception.InvalidStateException;
import com.sharif899.kyc.exception.ValidationException;
import com.sharif899.kyc.web.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Every error leaves this service in the same envelope as a success
 * ({@code success: false, error: {code, message, fieldErrors}}), so
 * clients only ever have to handle one shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Invalid value" : fieldError.getDefaultMessage(),
                        (first, second) -> first));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed", fieldErrors);
    }

    @ExceptionHandler({ValidationException.class, InvalidDocumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(ValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ApplicationNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateApplicationException ex) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_APPLICATION", ex.getMessage(),
                Map.of("existingApplicationId", ex.getExistingApplicationId().toString()));
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidState(InvalidStateException ex) {
        return error(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "'", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", Map.of());
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code,
                                                    String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(new ApiResponse.ErrorDetail(code, message, fieldErrors)));
    }
}
