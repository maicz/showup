package com.showup.api.service;

import com.showup.api.dto.CancelEventRequest;
import com.showup.api.dto.CreateEventRequest;
import com.showup.api.dto.EventDetail;
import com.showup.api.dto.EventSearchQuery;
import com.showup.api.dto.EventSummary;
import com.showup.api.dto.MemberSummary;
import com.showup.api.dto.PageResponse;
import com.showup.api.dto.RsvpSummary;
import com.showup.api.dto.UpdateEventRequest;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventHost;
import com.showup.api.entity.Group;
import com.showup.api.entity.Member;
import com.showup.api.entity.Money;
import com.showup.api.entity.Venue;
import com.showup.api.enums.EventFormat;
import com.showup.api.enums.EventHostRole;
import com.showup.api.enums.EventStatus;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ConflictException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.EventMapper;
import com.showup.api.mapper.MemberMapper;
import com.showup.api.mapper.RsvpMapper;
import com.showup.api.repository.EventHostRepository;
import com.showup.api.repository.EventRepository;
import com.showup.api.repository.RsvpRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EventService {

    /** No group currency is modelled yet, so a fee with no stated currency gets this one. */
    private static final String DEFAULT_CURRENCY = "USD";

    private final EventRepository events;
    private final EventHostRepository hosts;
    private final RsvpRepository rsvps;
    private final GroupService groupService;
    private final VenueService venueService;
    private final MemberService memberService;
    private final GroupAccessGuard guard;
    private final EventMapper eventMapper;
    private final MemberMapper memberMapper;
    private final RsvpMapper rsvpMapper;

    EventService(EventRepository events, EventHostRepository hosts, RsvpRepository rsvps,
                 GroupService groupService, VenueService venueService, MemberService memberService,
                 GroupAccessGuard guard, EventMapper eventMapper, MemberMapper memberMapper,
                 RsvpMapper rsvpMapper) {
        this.events = events;
        this.hosts = hosts;
        this.rsvps = rsvps;
        this.groupService = groupService;
        this.venueService = venueService;
        this.memberService = memberService;
        this.guard = guard;
        this.eventMapper = eventMapper;
        this.memberMapper = memberMapper;
        this.rsvpMapper = rsvpMapper;
    }

    public EventDetail create(UUID actorId, UUID groupId, CreateEventRequest request) {
        guard.requireEventAdmin(groupId, actorId);
        Group group = groupService.require(groupId);
        Event event = new Event(group, request.title(), request.format(), request.startsAt(),
                request.timeZone(), fee(request.feeAmountMinor(), request.feeCurrency()));
        apply(event, EventFields.of(request), groupId);
        events.save(event);
        // Whoever creates the event is its first public-facing host.
        hosts.save(new EventHost(event, memberService.require(actorId), EventHostRole.HOST));
        return detail(event.getId(), Optional.of(actorId));
    }

    public EventDetail update(UUID actorId, UUID eventId, UpdateEventRequest request) {
        Event event = require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("a cancelled event cannot be edited");
        }
        event.setFee(fee(request.feeAmountMinor(), request.feeCurrency()));
        apply(event, EventFields.of(request), event.getGroup().getId());
        return detail(eventId, Optional.of(actorId));
    }

    public EventDetail publish(UUID actorId, UUID eventId) {
        Event event = require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        if (event.getStatus() != EventStatus.DRAFT) {
            // A separate endpoint per transition exists precisely so CANCELLED -> PUBLISHED is
            // unrepresentable rather than merely discouraged. See docs/domain-model.md#requests.
            throw new BusinessRuleException("only a DRAFT event can be published; this one is "
                    + event.getStatus());
        }
        event.setStatus(EventStatus.PUBLISHED);
        return detail(eventId, Optional.of(actorId));
    }

    public EventDetail cancel(UUID actorId, UUID eventId, CancelEventRequest request) {
        Event event = require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ConflictException("event is already cancelled");
        }
        event.cancel(request.reason());
        return detail(eventId, Optional.of(actorId));
    }

    @Transactional(readOnly = true)
    public EventDetail detail(UUID eventId, Optional<UUID> viewerId) {
        Event event = require(eventId);
        List<MemberSummary> eventHosts = hosts.findAllByEventId(eventId).stream()
                .map(EventHost::getMember)
                .map(memberMapper::toSummary)
                .toList();
        // Folded in here so a signed-in client can render the RSVP button without a second call.
        RsvpSummary viewerRsvp = viewerId
                .flatMap(id -> rsvps.findByEventIdAndMemberId(eventId, id))
                .map(rsvpMapper::toSummary)
                .orElse(null);
        EventDetail detail = eventMapper.toDetail(event, eventHosts, viewerRsvp);
        boolean canViewOnlineLink = (viewerRsvp != null && viewerRsvp.status() == RsvpStatus.YES)
                || viewerId.map(id -> guard.isEventAdmin(event.getGroup().getId(), id)
                        || hosts.existsByEventIdAndMemberId(eventId, id))
                        .orElse(false);
        if (!canViewOnlineLink && detail.onlineUrl() != null) {
            return detail.withoutOnlineUrl();
        }
        return detail;
    }

    @Transactional(readOnly = true)
    public List<EventSummary> listPublished() {
        return events.findAllByStatusOrderByStartsAtAsc(EventStatus.PUBLISHED).stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<EventSummary> search(EventSearchQuery query) {
        Page<Event> found = events.search(query);
        return PageResponse.of(
                found.getContent().stream().map(eventMapper::toSummary).toList(),
                found.getNumber(), found.getSize(), found.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<EventSummary> byGroup(UUID groupId) {
        return events.findAllByGroupIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                        groupId, EventStatus.PUBLISHED, Instant.now()).stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    public List<MemberSummary> addHost(UUID actorId, UUID eventId, UUID memberId, EventHostRole role) {
        Event event = require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        if (hosts.existsByEventIdAndMemberId(eventId, memberId)) {
            throw new ConflictException("member " + memberId + " already hosts this event");
        }
        Member member = memberService.require(memberId);
        guard.requireActiveMember(event.getGroup().getId(), memberId);
        hosts.save(new EventHost(event, member, role));
        return hostSummaries(eventId);
    }

    public List<MemberSummary> removeHost(UUID actorId, UUID eventId, UUID memberId) {
        Event event = require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        EventHost host = hosts.findByEventIdAndMemberId(eventId, memberId)
                .orElseThrow(() -> new NotFoundException("member " + memberId + " does not host this event"));
        if (hosts.findAllByEventId(eventId).size() <= 1) {
            throw new BusinessRuleException("an event must keep at least one host");
        }
        hosts.delete(host);
        return hostSummaries(eventId);
    }

    Event require(UUID eventId) {
        return events.findById(eventId).orElseThrow(() -> NotFoundException.of("event", eventId));
    }

    private List<MemberSummary> hostSummaries(UUID eventId) {
        return hosts.findAllByEventId(eventId).stream()
                .map(EventHost::getMember)
                .map(memberMapper::toSummary)
                .toList();
    }

    private static Money fee(Long amountMinor, String currency) {
        return new Money(amountMinor == null ? 0L : amountMinor, currency == null ? DEFAULT_CURRENCY : currency);
    }

    private void apply(Event event, EventFields fields, UUID groupId) {
        event.setTitle(fields.title());
        event.setDescription(fields.description());
        event.setFormat(fields.format());
        event.setVenue(venueFor(fields.venueId(), groupId));
        event.setOnlineUrl(fields.onlineUrl());
        event.setStartsAt(fields.startsAt());
        event.setEndsAt(fields.endsAt());
        event.setTimeZone(fields.timeZone());
        event.setCapacity(fields.capacity());
        event.setWaitlistEnabled(fields.waitlistEnabled());
        event.setGuestsPerRsvpLimit(fields.guestsPerRsvpLimit());
        event.setRsvpOpensAt(fields.rsvpOpensAt());
        event.setRsvpClosesAt(fields.rsvpClosesAt());
    }

    /**
     * A venue belongs to the group that entered it. Without this check an organizer could point an
     * event at any venue id in the system, including a private one from another group.
     */
    private Venue venueFor(UUID venueId, UUID groupId) {
        if (venueId == null) {
            return null;
        }
        Venue venue = venueService.require(venueId);
        if (!venue.getCreatedByGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("venue " + venueId + " belongs to another group");
        }
        return venue;
    }

    /**
     * {@code CreateEventRequest} and {@code UpdateEventRequest} are separate types on purpose — a
     * create may set fields an update must not, and they will drift. This is the one place that
     * needs them to look alike, so they are normalized here rather than merged upstream.
     */
    private record EventFields(
            String title, String description, EventFormat format, UUID venueId, String onlineUrl,
            Instant startsAt, Instant endsAt, String timeZone, Integer capacity, boolean waitlistEnabled,
            int guestsPerRsvpLimit, Instant rsvpOpensAt, Instant rsvpClosesAt) {

        static EventFields of(CreateEventRequest r) {
            return new EventFields(r.title(), r.description(), r.format(), r.venueId(), r.onlineUrl(),
                    r.startsAt(), r.endsAt(), r.timeZone(), r.capacity(), r.waitlistEnabled(),
                    r.guestsPerRsvpLimit(), r.rsvpOpensAt(), r.rsvpClosesAt());
        }

        static EventFields of(UpdateEventRequest r) {
            return new EventFields(r.title(), r.description(), r.format(), r.venueId(), r.onlineUrl(),
                    r.startsAt(), r.endsAt(), r.timeZone(), r.capacity(), r.waitlistEnabled(),
                    r.guestsPerRsvpLimit(), r.rsvpOpensAt(), r.rsvpClosesAt());
        }
    }
}
