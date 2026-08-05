package com.showup.api.dto;

import com.showup.api.enums.RsvpStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SubmitRsvpRequest(
        @NotNull RsvpStatus status,
        @PositiveOrZero int guestCount) {
}
