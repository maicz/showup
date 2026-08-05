package com.showup.api.controller;

import com.showup.api.dto.CreateEventSeriesRequest;
import com.showup.api.dto.EventSeriesDetail;
import com.showup.api.dto.EventSummary;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.EventSeriesService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventSeriesController {

    private final EventSeriesService series;

    EventSeriesController(EventSeriesService series) {
        this.series = series;
    }

    @PostMapping("/event-series")
    @ResponseStatus(HttpStatus.CREATED)
    public EventSeriesDetail create(@CurrentMember UUID actor,
                                    @Valid @RequestBody CreateEventSeriesRequest request) {
        return series.create(actor, request);
    }

    @GetMapping("/event-series/{id}")
    public EventSeriesDetail byId(@PathVariable UUID id) {
        return series.detail(id);
    }

    @GetMapping("/groups/{groupId}/event-series")
    public List<EventSeriesDetail> byGroup(@PathVariable UUID groupId) {
        return series.byGroup(groupId);
    }

    @GetMapping("/event-series/{id}/occurrences")
    public List<EventSummary> occurrences(@PathVariable UUID id) {
        return series.occurrences(id);
    }

    /**
     * Materializes occurrence rows from the recurrence rule, in {@code DRAFT}. Idempotent: an
     * occurrence that already exists at the same instant is skipped rather than duplicated.
     */
    @PostMapping("/event-series/{id}/occurrences")
    @ResponseStatus(HttpStatus.CREATED)
    public List<EventSummary> materialize(@CurrentMember UUID actor, @PathVariable UUID id,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                          Instant firstStartsAt,
                                          @RequestParam(defaultValue = "12") int count) {
        return series.materialize(actor, id, firstStartsAt, count);
    }
}
