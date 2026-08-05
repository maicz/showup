package com.showup.api.controller;

import com.showup.api.dto.ApiError;
import com.showup.api.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direct unit tests of the exception-to-status mapping, bypassing HTTP entirely. Several branches
 * here — a database constraint firing after a service's own pre-check lost a race, two writers
 * touching the same optimistically-locked row, a truly unexpected exception — are impractical to
 * provoke through a real request, so the handler methods (package-private, callable from this
 * same-package test) are exercised directly instead.
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/whatever");
    }

    @Test
    void notFoundMapsTo404() {
        ResponseEntity<ApiError> response = handler.notFound(NotFoundException.of("event", java.util.UUID.randomUUID()), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("not found");
    }

    @Test
    void aBusinessRuleViolationMapsTo422NotABareBadRequest() {
        ResponseEntity<ApiError> response = handler.businessRule(
                new com.showup.api.exception.BusinessRuleException("event is DRAFT"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void notImplementedMapsTo501() {
        ResponseEntity<ApiError> response = handler.notImplemented(
                new com.showup.api.exception.NotImplementedException("sso not wired up"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    void fieldErrorsAndClassLevelErrorsAreBothReportedByInvalidBody() {
        FieldError fieldError = new FieldError("createEventRequest", "title", "must not be blank");
        ObjectError globalError = new ObjectError("createEventRequest", "endsAt must be after startsAt");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(bindingResult.getGlobalErrors()).thenReturn(List.of(globalError));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> response = handler.invalidBody(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().fieldErrors()).hasSize(2);
        assertThat(response.getBody().fieldErrors().get(0).field()).isEqualTo("title");
        assertThat(response.getBody().fieldErrors().get(1).field()).isEqualTo("createEventRequest");
    }

    @Test
    void aFieldErrorWithNoDefaultMessageFallsBackToAGenericOne() {
        FieldError fieldError = new FieldError("createEventRequest", "title", null, false, null, null, null);
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(bindingResult.getGlobalErrors()).thenReturn(List.of());
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> response = handler.invalidBody(exception, request);

        assertThat(response.getBody().fieldErrors().get(0).message()).isEqualTo("is invalid");
    }

    @Test
    void constraintViolationsOnRequestParametersAreReported() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("groupActivity.from");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");

        ResponseEntity<ApiError> response = handler.invalidParams(
                new ConstraintViolationException(Set.of(violation)), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().fieldErrors().get(0).field()).isEqualTo("groupActivity.from");
    }

    @Test
    void malformedInputVariantsAllMapToBadRequest() {
        assertThat(handler.malformed(new IllegalArgumentException("bad enum value"), request).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aDatabaseConstraintLostRaceMapsToConflictNotA500() {
        ResponseEntity<ApiError> response = handler.integrity(
                mock(DataIntegrityViolationException.class), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("conflicts with existing data");
    }

    @Test
    void aLostOptimisticLockMapsToConflictWithARetryHint() {
        ResponseEntity<ApiError> response = handler.optimisticLock(
                mock(ObjectOptimisticLockingFailureException.class), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("retry");
    }

    @Test
    void anUnexpectedExceptionNeverLeaksItsMessageToTheClient() {
        ResponseEntity<ApiError> response = handler.unexpected(
                new RuntimeException("stack trace with internal package names"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("internal server error");
    }
}
