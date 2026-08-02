package com.showup.api.rsvp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SubmitRsvpRequest(
        @NotNull RsvpStatus status,
        @PositiveOrZero int guestCount) {
}
