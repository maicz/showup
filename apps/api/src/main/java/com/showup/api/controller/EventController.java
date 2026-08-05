package com.showup.api.controller;

import com.showup.api.dto.CancelEventRequest;
import com.showup.api.dto.CreateEventRequest;
import com.showup.api.dto.EventDetail;
import com.showup.api.dto.EventSearchQuery;
import com.showup.api.dto.EventSummary;
import com.showup.api.dto.MemberSummary;
import com.showup.api.dto.PageResponse;
import com.showup.api.dto.UpdateEventRequest;
import com.showup.api.enums.EventHostRole;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService events;

    EventController(EventService events) {
        this.events = events;
    }

    @GetMapping("/events")
    public List<EventSummary> list() {
        return events.listPublished();
    }

    /** The faceted browse: distance, category, topics, format, date window, price, availability. */
    @GetMapping("/events/search")
    public PageResponse<EventSummary> search(@ModelAttribute EventSearchQuery query) {
        return events.search(query);
    }

    /**
     * Public, but richer when signed in — {@code viewerRsvp} is populated from the bearer token
     * if one was sent, which is why the parameter is {@code Optional} rather than required.
     */
    @GetMapping("/events/{id}")
    public EventDetail byId(@PathVariable UUID id, @CurrentMember Optional<UUID> viewer) {
        return events.detail(id, viewer);
    }

    @PostMapping("/groups/{groupId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDetail create(@CurrentMember UUID actor, @PathVariable UUID groupId,
                              @Valid @RequestBody CreateEventRequest request) {
        return events.create(actor, groupId, request);
    }

    @PutMapping("/events/{id}")
    public EventDetail update(@CurrentMember UUID actor, @PathVariable UUID id,
                              @Valid @RequestBody UpdateEventRequest request) {
        return events.update(actor, id, request);
    }

    // State transitions get their own endpoints rather than a patchable status field, so
    // CANCELLED -> DRAFT is not expressible. See docs/domain-model.md#requests.

    @PostMapping("/events/{id}/publish")
    public EventDetail publish(@CurrentMember UUID actor, @PathVariable UUID id) {
        return events.publish(actor, id);
    }

    @PostMapping("/events/{id}/cancel")
    public EventDetail cancel(@CurrentMember UUID actor, @PathVariable UUID id,
                              @Valid @RequestBody CancelEventRequest request) {
        return events.cancel(actor, id, request);
    }

    @PostMapping("/events/{id}/hosts")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MemberSummary> addHost(@CurrentMember UUID actor, @PathVariable UUID id,
                                       @RequestParam UUID memberId,
                                       @RequestParam(defaultValue = "CO_HOST") EventHostRole role) {
        return events.addHost(actor, id, memberId, role);
    }

    @DeleteMapping("/events/{id}/hosts/{memberId}")
    public List<MemberSummary> removeHost(@CurrentMember UUID actor, @PathVariable UUID id,
                                          @PathVariable UUID memberId) {
        return events.removeHost(actor, id, memberId);
    }
}
