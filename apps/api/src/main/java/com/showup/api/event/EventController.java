package com.showup.api.event;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository events;
    private final EventMapper mapper;

    EventController(EventRepository events, EventMapper mapper) {
        this.events = events;
        this.mapper = mapper;
    }

    @GetMapping
    public List<EventSummary> list() {
        return events.findAllByStatusOrderByStartsAtAsc(EventStatus.PUBLISHED).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetail> byId(@PathVariable UUID id) {
        return events.findById(id)
                // hosts/viewerRsvp need the event-host and RSVP tables joined in — left for the
                // service layer that will back this endpoint; not wired up yet.
                .map(event -> mapper.toDetail(event, List.of(), null))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
