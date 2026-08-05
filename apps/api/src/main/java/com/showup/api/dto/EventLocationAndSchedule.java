package com.showup.api.dto;

import com.showup.api.enums.EventFormat;
import com.showup.api.validation.ValidEventRequestValidator;

import java.time.Instant;
import java.util.UUID;

/** The subset of fields {@link ValidEventRequestValidator} needs, shared by create and update requests. */
public interface EventLocationAndSchedule {

    EventFormat format();

    UUID venueId();

    String onlineUrl();

    Instant startsAt();

    Instant endsAt();

    String timeZone();
}
