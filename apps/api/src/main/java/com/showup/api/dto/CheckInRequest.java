package com.showup.api.dto;

import com.showup.api.enums.CheckInMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckInRequest(
        @NotBlank String ticketCode,
        @NotNull CheckInMethod method,
        @Positive int admittedCount) {
}
