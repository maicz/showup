package com.showup.api.event;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.DateTimeException;
import java.time.ZoneId;

class ValidEventRequestValidator implements ConstraintValidator<ValidEventRequest, EventLocationAndSchedule> {

    @Override
    public boolean isValid(EventLocationAndSchedule request, ConstraintValidatorContext context) {
        if (request == null || request.format() == null) {
            return true;
        }
        if (request.format() != EventFormat.ONLINE && request.venueId() == null) {
            return false;
        }
        if (request.format() != EventFormat.IN_PERSON
                && (request.onlineUrl() == null || request.onlineUrl().isBlank())) {
            return false;
        }
        if (request.startsAt() != null && request.endsAt() != null
                && !request.endsAt().isAfter(request.startsAt())) {
            return false;
        }
        if (request.timeZone() != null) {
            try {
                ZoneId.of(request.timeZone());
            } catch (DateTimeException e) {
                return false;
            }
        }
        return true;
    }
}
