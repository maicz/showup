package com.showup.api.venue;

import com.showup.api.shared.GeoPoint;

import java.util.UUID;

public record VenueSummary(
        UUID id,
        String name,
        String addressLine1,
        String city,
        String region,
        String postalCode,
        String country,
        GeoPoint location) {
}
