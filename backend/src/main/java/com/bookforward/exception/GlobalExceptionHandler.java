package com.bookforward.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Single place that turns exceptions into the predictable API error format. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException e, HttpServletRequest r) {
        return build(e.getStatus(), e.getCode(), e.getMessage(), r, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e, HttpServletRequest r) {
        var issues = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldIssue(f.getField(), f.getDefaultMessage())).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Please correct the highlighted fields", r, issues);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraint(ConstraintViolationException e, HttpServletRequest r) {
        var issues = e.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldIssue(v.getPropertyPath().toString(), v.getMessage())).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request parameters", r, issues);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> malformed(Exception e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request could not be understood", r, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> denied(AccessDeniedException e, HttpServletRequest r) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to do that", r, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException e, HttpServletRequest r) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "The uploaded file is too large (max 5 MB)", r, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e, HttpServletRequest r) {
        log.warn("Data integrity violation on {}: {}", r.getRequestURI(), e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "DUPLICATE_OR_CONFLICT", "That record already exists or conflicts with existing data", r, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> method(HttpRequestMethodNotSupportedException e, HttpServletRequest r) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed", r, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException e, HttpServletRequest r) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", r, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
        log.error("Unhandled error on {} {}", r.getMethod(), r.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong. Please try again later.", r, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus s, String code, String msg, HttpServletRequest r,
                                                List<ErrorResponse.FieldIssue> issues) {
        return ResponseEntity.status(s).body(new ErrorResponse(Instant.now(), s.value(), code, msg,
                r.getRequestURI(), issues, MDC.get("cid")));
    }
}
