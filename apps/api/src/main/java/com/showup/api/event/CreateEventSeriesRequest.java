package com.showup.api.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record CreateEventSeriesRequest(
        @NotNull UUID groupId,
        @NotBlank String recurrenceRule,
        Instant until,
        @NotBlank String templateTitle,
        String templateDescription,
        UUID templateVenueId,
        @Positive int templateDurationMinutes) {
}
