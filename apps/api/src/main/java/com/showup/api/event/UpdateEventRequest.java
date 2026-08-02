package com.showup.api.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** Same shape as {@link CreateEventRequest} — title/schedule/pricing can all be edited pre-publish. */
@ValidEventRequest
public record UpdateEventRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 20_000) String description,
        @NotNull EventFormat format,
        UUID venueId,
        @Size(max = 500) String onlineUrl,
        @NotNull Instant startsAt,
        Instant endsAt,
        @NotBlank String timeZone,
        @Positive Integer capacity,
        boolean waitlistEnabled,
        @PositiveOrZero int guestsPerRsvpLimit,
        @PositiveOrZero long feeAmountMinor,
        @Pattern(regexp = "[A-Z]{3}") String feeCurrency,
        Instant rsvpOpensAt, Instant rsvpClosesAt) implements EventLocationAndSchedule {
}
