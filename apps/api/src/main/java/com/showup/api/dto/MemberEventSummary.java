package com.showup.api.dto;

import com.showup.api.enums.AvailabilityState;
import com.showup.api.enums.EventFormat;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.entity.Money;

import java.time.Instant;
import java.util.UUID;

/** An event on the member dashboard, enriched with that member's own RSVP state. */
public record MemberEventSummary(
        UUID id,
        String title,
        Instant startsAt,
        String timeZone,
        EventFormat format,
        String venueCity,
        Money fee,
        int yesRsvpCount,
        Integer capacity,
        AvailabilityState availability,
        GroupSummary group,
        RsvpStatus rsvpStatus,
        int guestCount,
        Integer waitlistPosition) {

    public static MemberEventSummary of(EventSummary event, RsvpSummary rsvp) {
        return new MemberEventSummary(
                event.id(), event.title(), event.startsAt(), event.timeZone(), event.format(),
                event.venueCity(), event.fee(), event.yesRsvpCount(), event.capacity(),
                event.availability(), event.group(), rsvp.status(), rsvp.guestCount(),
                rsvp.waitlistPosition());
    }
}
