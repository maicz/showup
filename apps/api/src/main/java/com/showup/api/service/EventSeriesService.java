package com.showup.api.service;

import com.showup.api.dto.CreateEventSeriesRequest;
import com.showup.api.dto.EventSeriesDetail;
import com.showup.api.dto.EventSummary;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventSeries;
import com.showup.api.entity.Group;
import com.showup.api.entity.Money;
import com.showup.api.entity.Venue;
import com.showup.api.enums.EventFormat;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.EventMapper;
import com.showup.api.mapper.EventSeriesMapper;
import com.showup.api.repository.EventRepository;
import com.showup.api.repository.EventSeriesRepository;
import com.showup.api.util.RecurrenceRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Recurrence. Occurrences are materialized into real {@code event} rows rather than computed on
 * read, so each one can be individually cancelled, re-venued, RSVPed to, and reported on — see
 * docs/domain-model.md#eventseries.
 */
@Service
@Transactional
public class EventSeriesService {

    /** Ceiling on one materialize call, so an unbounded rule cannot fill the table. */
    private static final int MAX_OCCURRENCES = 52;

    private static final String DEFAULT_CURRENCY = "USD";

    private final EventSeriesRepository series;
    private final EventRepository events;
    private final GroupService groupService;
    private final VenueService venueService;
    private final GroupAccessGuard guard;
    private final EventSeriesMapper mapper;
    private final EventMapper eventMapper;

    EventSeriesService(EventSeriesRepository series, EventRepository events, GroupService groupService,
                       VenueService venueService, GroupAccessGuard guard, EventSeriesMapper mapper,
                       EventMapper eventMapper) {
        this.series = series;
        this.events = events;
        this.groupService = groupService;
        this.venueService = venueService;
        this.guard = guard;
        this.mapper = mapper;
        this.eventMapper = eventMapper;
    }

    public EventSeriesDetail create(UUID actorId, CreateEventSeriesRequest request) {
        guard.requireEventAdmin(request.groupId(), actorId);
        // Parse now so a malformed rule is a 422 on create, not a surprise at materialize time.
        parse(request.recurrenceRule());

        Group group = groupService.require(request.groupId());
        EventSeries created = new EventSeries(group, request.recurrenceRule(),
                request.templateTitle(), request.templateDurationMinutes());
        created.setTemplateDescription(request.templateDescription());
        created.setUntil(request.until());
        if (request.templateVenueId() != null) {
            created.setTemplateVenue(venue(request.templateVenueId(), request.groupId()));
        }
        return mapper.toDetail(series.save(created));
    }

    @Transactional(readOnly = true)
    public EventSeriesDetail detail(UUID seriesId) {
        return mapper.toDetail(require(seriesId));
    }

    @Transactional(readOnly = true)
    public List<EventSeriesDetail> byGroup(UUID groupId) {
        return series.findAllByGroupId(groupId).stream().map(mapper::toDetail).toList();
    }

    @Transactional(readOnly = true)
    public List<EventSummary> occurrences(UUID seriesId) {
        return events.findAllBySeriesIdOrderByStartsAtAsc(seriesId).stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    /**
     * Creates the occurrence rows, in {@code DRAFT} so an organizer reviews them before they go
     * public. Occurrences that already exist at the same instant are skipped, which makes calling
     * this twice safe.
     */
    public List<EventSummary> materialize(UUID actorId, UUID seriesId, Instant firstStartsAt, int count) {
        EventSeries eventSeries = require(seriesId);
        Group group = eventSeries.getGroup();
        guard.requireEventAdmin(group.getId(), actorId);

        ZoneId zone = ZoneId.of(group.getTimeZone());
        List<Instant> starts = RecurrenceRules.expand(parse(eventSeries.getRecurrenceRule()),
                firstStartsAt, zone, eventSeries.getUntil(), Math.clamp(count, 1, MAX_OCCURRENCES));

        List<Instant> existing = events.findAllBySeriesIdOrderByStartsAtAsc(seriesId).stream()
                .map(Event::getStartsAt)
                .toList();

        List<Event> created = starts.stream()
                .filter(start -> !existing.contains(start))
                .map(start -> occurrence(eventSeries, group, zone, start))
                .map(events::save)
                .toList();
        return created.stream().map(eventMapper::toSummary).toList();
    }

    EventSeries require(UUID seriesId) {
        return series.findById(seriesId).orElseThrow(() -> NotFoundException.of("event series", seriesId));
    }

    private Event occurrence(EventSeries eventSeries, Group group, ZoneId zone, Instant startsAt) {
        // The series has no format column; a template venue is what distinguishes the two cases.
        EventFormat format = eventSeries.getTemplateVenue() == null ? EventFormat.ONLINE : EventFormat.IN_PERSON;
        Event event = new Event(group, eventSeries.getTemplateTitle(), format, startsAt,
                zone.getId(), Money.zero(DEFAULT_CURRENCY));
        event.setSeries(eventSeries);
        event.setDescription(eventSeries.getTemplateDescription());
        event.setVenue(eventSeries.getTemplateVenue());
        event.setEndsAt(startsAt.plusSeconds(eventSeries.getTemplateDurationMinutes() * 60L));
        return event;
    }

    private Venue venue(UUID venueId, UUID groupId) {
        Venue venue = venueService.require(venueId);
        if (!venue.getCreatedByGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("venue " + venueId + " belongs to another group");
        }
        return venue;
    }

    private static RecurrenceRules.Rule parse(String rrule) {
        try {
            return RecurrenceRules.parse(rrule);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(e.getMessage());
        }
    }
}
