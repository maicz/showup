package com.showup.api.event;

import com.showup.api.group.GroupSummary;
import com.showup.api.shared.Money;

import java.time.Instant;
import java.util.UUID;

/** List card — mirrors exactly what Meetup's search results render. */
public record EventSummary(
        UUID id, String title, Instant startsAt, String timeZone,
        EventFormat format, String venueCity,
        Money fee, int yesRsvpCount, Integer capacity,
        AvailabilityState availability,
        GroupSummary group) {
}
