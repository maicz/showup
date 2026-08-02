package com.showup.api.event;

/**
 * Computed server-side from {@code capacity}, {@code yesRsvpCount}, and {@code waitlistEnabled}
 * so every client renders the same Full / Waitlist / Seats-available badge. See
 * docs/domain-model.md#shape-per-endpoint.
 */
public enum AvailabilityState {
    SEATS_AVAILABLE,
    WAITLIST,
    FULL
}
