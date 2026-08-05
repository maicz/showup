package com.showup.api.dto;

import com.showup.api.entity.Money;
import com.showup.api.enums.AvailabilityState;
import com.showup.api.enums.EventFormat;

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
