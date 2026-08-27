package com.showup.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVenueRequest(
        @NotBlank(message = "Venue name is required")
        @Size(max = 200, message = "Venue name cannot exceed 200 characters")
        String name,

        @NotBlank(message = "Street address is required")
        @Size(max = 200, message = "Street address cannot exceed 200 characters")
        String addressLine1,

        @Size(max = 200, message = "Address line 2 cannot exceed 200 characters")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "Region/State cannot exceed 100 characters")
        String region,

        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country cannot exceed 100 characters")
        String country,

        GeoPoint location,

        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String notes) {
}
