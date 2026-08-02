package com.showup.api.event;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String location,
        Instant startsAt,
        int capacity) {

    static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartsAt(),
                event.getCapacity());
    }
}
