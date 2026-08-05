package com.showup.api.controller;

import com.showup.api.dto.ApiError;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ConflictException;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.exception.NotImplementedException;
import com.showup.api.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/**
 * The one place HTTP status codes are chosen. Services throw domain exceptions and never mention
 * {@code ResponseEntity}, so the same rule returns the same status from every endpoint that
 * triggers it, and a new controller cannot invent its own error shape.
 *
 * <p>Security rejections happen earlier, in the filter chain — see
 * {@code ApiErrorAuthenticationHandler}, which emits the same {@link ApiError} body.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiError> forbidden(ForbiddenException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> unauthorized(UnauthorizedException e, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    /**
     * 422, not 400: the body parsed and passed bean validation. It is the domain state machine
     * that refused — RSVPs closed, event already cancelled, waitlist disabled.
     */
    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> businessRule(BusinessRuleException e, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), request);
    }

    @ExceptionHandler(NotImplementedException.class)
    ResponseEntity<ApiError> notImplemented(NotImplementedException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_IMPLEMENTED, e.getMessage(), request);
    }

    /** Bean-validation failures on a request body, reported field by field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ApiError.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(field -> new ApiError.FieldError(field.getField(), message(field)))
                .toList();
        // Class-level constraints such as @ValidEventRequest have no field to attach to.
        List<ApiError.FieldError> global = e.getBindingResult().getGlobalErrors().stream()
                .map(error -> new ApiError.FieldError(error.getObjectName(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "request validation failed", request.getRequestURI(),
                java.util.stream.Stream.concat(fields.stream(), global.stream()).toList()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> invalidParams(ConstraintViolationException e, HttpServletRequest request) {
        List<ApiError.FieldError> fields = e.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "request validation failed", request.getRequestURI(), fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    ResponseEntity<ApiError> malformed(Exception e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    /**
     * The database's unique constraints are the real guarantee behind several service checks —
     * duplicate RSVPs, duplicate memberships. When one fires anyway (a race the pre-check lost),
     * it is still a conflict, not a 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("constraint violation on {}", request.getRequestURI(), e);
        return error(HttpStatus.CONFLICT, "the request conflicts with existing data", request);
    }

    /** Two writers touched the same row; the loser retries. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> optimisticLock(ObjectOptimisticLockingFailureException e,
                                            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "the record changed while you were editing it; retry", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        // The message may name internals, so it is logged rather than returned.
        log.error("unhandled exception on {}", request.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error", request);
    }

    private static String message(FieldError field) {
        return field.getDefaultMessage() == null ? "is invalid" : field.getDefaultMessage();
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), List.of()));
    }
}
