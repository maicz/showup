package com.showup.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVenueRequest(
        @NotBlank String name,
        @NotBlank String addressLine1,
        String addressLine2,
        @NotBlank String city,
        String region,
        String postalCode,
        @NotBlank String country,
        @NotNull GeoPoint location,
        String notes) {
}
