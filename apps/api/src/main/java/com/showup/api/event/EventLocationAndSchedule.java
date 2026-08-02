package com.showup.api.event;

import java.time.Instant;
import java.util.UUID;

/** The subset of fields {@link ValidEventRequestValidator} needs, shared by create and update requests. */
interface EventLocationAndSchedule {

    EventFormat format();

    UUID venueId();

    String onlineUrl();

    Instant startsAt();

    Instant endsAt();

    String timeZone();
}
