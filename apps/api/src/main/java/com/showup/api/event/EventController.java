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

    EventController(EventRepository events) {
        this.events = events;
    }

    @GetMapping
    public List<EventResponse> list() {
        return events.findAllByOrderByStartsAtAsc().stream()
                .map(EventResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> byId(@PathVariable UUID id) {
        return events.findById(id)
                .map(EventResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
