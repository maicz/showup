package com.showup.api.event;

import com.showup.api.group.GroupSummary;
import com.showup.api.member.MemberSummary;
import com.showup.api.rsvp.RsvpSummary;
import com.showup.api.shared.Money;
import com.showup.api.venue.VenueSummary;

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
}
