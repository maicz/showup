package com.showup.api.shared;

/**
 * Wire/DTO-level latitude-longitude pair (WGS84). Entities persist the equivalent
 * {@code geography(Point)} column as a JTS {@code Point} via Hibernate Spatial; see
 * {@link GeoPoints} for the conversion between the two.
 */
public record GeoPoint(double latitude, double longitude) {

    public GeoPoint {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180: " + longitude);
        }
    }
}
