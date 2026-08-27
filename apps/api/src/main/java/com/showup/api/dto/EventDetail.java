package com.showup.api.dto;

import com.showup.api.entity.Money;
import com.showup.api.enums.AvailabilityState;
import com.showup.api.enums.EventFormat;
import com.showup.api.enums.EventStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventDetail(
        UUID id, String title, String description, EventStatus status,
        EventFormat format, VenueSummary venue, String onlineUrl,
        Instant startsAt, Instant endsAt, String timeZone,
        Integer capacity, boolean waitlistEnabled, int guestsPerRsvpLimit,
        Money fee, Instant rsvpOpensAt, Instant rsvpClosesAt,
        int yesRsvpCount, int waitlistCount, AvailabilityState availability,
        GroupSummary group, List<MemberSummary> hosts,
        RsvpSummary viewerRsvp) {   // null when not signed in

    public EventDetail withoutOnlineUrl() {
        return new EventDetail(
                id, title, description, status, format, venue, null,
                startsAt, endsAt, timeZone, capacity, waitlistEnabled,
                guestsPerRsvpLimit, fee, rsvpOpensAt, rsvpClosesAt,
                yesRsvpCount, waitlistCount, availability, group, hosts, viewerRsvp);
    }
}
