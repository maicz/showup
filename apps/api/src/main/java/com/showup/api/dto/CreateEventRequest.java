package com.showup.api.dto;

import com.showup.api.enums.EventFormat;
import com.showup.api.validation.ValidEventRequest;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@ValidEventRequest
public record CreateEventRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 20_000) String description,
        @NotNull EventFormat format,
        UUID venueId,
        @Size(max = 500) String onlineUrl,
        @NotNull @Future Instant startsAt,
        Instant endsAt,
        @NotBlank String timeZone,
        @Positive Integer capacity,
        boolean waitlistEnabled,
        @PositiveOrZero int guestsPerRsvpLimit,
        @PositiveOrZero Long feeAmountMinor,
        @Pattern(regexp = "[A-Z]{3}") String feeCurrency,
        Instant rsvpOpensAt, Instant rsvpClosesAt) implements EventLocationAndSchedule {
}
