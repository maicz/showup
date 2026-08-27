package com.showup.api.service;

import com.showup.api.dto.AttendeeSummary;
import com.showup.api.dto.EventSummary;
import com.showup.api.dto.MemberEventSummary;
import com.showup.api.dto.RsvpSummary;
import com.showup.api.dto.SubmitRsvpRequest;
import com.showup.api.entity.Event;
import com.showup.api.entity.Rsvp;
import com.showup.api.enums.EventStatus;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.EventMapper;
import com.showup.api.mapper.RsvpMapper;
import com.showup.api.repository.EventRepository;
import com.showup.api.repository.RsvpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Seat allocation. Every method that can change {@code yes_rsvp_count} starts by taking a row
 * lock on the event via {@link EventRepository#findByIdForUpdate}, because the decision to seat
 * or waitlist is a read-then-write on that counter: two concurrent RSVPs that both read the
 * pre-increment value would both be seated and the event would oversell.
 *
 * <p>{@code yesRsvpCount} counts <em>seats</em>, not rows — a member bringing two guests holds
 * three of them — because {@code capacity} is a headcount. {@code waitlistCount} counts rows,
* since a waitlist position belongs to an RSVP.
 */
@Service
@Transactional
public class RsvpService {

    private final RsvpRepository rsvps;
    private final EventRepository events;
    private final EventService eventService;
    private final MemberService memberService;
    private final GroupAccessGuard guard;
    private final RsvpMapper mapper;
    private final EventMapper eventMapper;

    RsvpService(RsvpRepository rsvps, EventRepository events, EventService eventService,
                MemberService memberService, GroupAccessGuard guard, RsvpMapper mapper,
                EventMapper eventMapper) {
        this.rsvps = rsvps;
        this.events = events;
        this.eventService = eventService;
        this.memberService = memberService;
        this.guard = guard;
        this.mapper = mapper;
        this.eventMapper = eventMapper;
    }

    public RsvpSummary submit(UUID actorId, UUID eventId, SubmitRsvpRequest request) {
        if (request.status() == RsvpStatus.WAITLISTED) {
            throw new BusinessRuleException(
                    "waitlist placement is decided by the server; submit YES and you will be "
                            + "waitlisted if the event is full");
        }
        Event event = lock(eventId);
        requireRsvpOpen(event);
        if (request.guestCount() > event.getGuestsPerRsvpLimit()) {
            throw new BusinessRuleException("this event allows at most "
                    + event.getGuestsPerRsvpLimit() + " guests per RSVP");
        }

        Rsvp rsvp = rsvps.findByEventIdAndMemberId(eventId, actorId).orElse(null);
        release(event, rsvp);

        if (request.status() == RsvpStatus.NO) {
            rsvp = upsert(event, rsvp, actorId, RsvpStatus.NO, 0);
            rsvp.setWaitlistPosition(null);
            promoteFromWaitlist(event);
            return mapper.toSummary(rsvp);
        }

        int seats = 1 + request.guestCount();
        if (hasRoomFor(event, seats)) {
            rsvp = upsert(event, rsvp, actorId, RsvpStatus.YES, request.guestCount());
            if (rsvp.getWaitlistPosition() != null) {
                rsvp.setWaitlistPosition(null);
                rsvp.setPromotedAt(Instant.now());
            }
            event.setYesRsvpCount(event.getYesRsvpCount() + seats);
        } else if (event.isWaitlistEnabled()) {
            boolean alreadyWaiting = rsvp != null && rsvp.getStatus() == RsvpStatus.WAITLISTED;
            rsvp = upsert(event, rsvp, actorId, RsvpStatus.WAITLISTED, request.guestCount());
            if (!alreadyWaiting) {
                rsvp.setWaitlistPosition(rsvps.findMaxWaitlistPosition(eventId) + 1);
                event.setWaitlistCount(event.getWaitlistCount() + 1);
            }
        } else {
            throw new BusinessRuleException("event is full and has no waitlist");
        }
        // Seats released above may now fit someone who was already waiting.
        promoteFromWaitlist(event);
        return mapper.toSummary(rsvp);
    }

    /** Withdrawing is the same transaction as answering NO, so it goes through the same path. */
    public RsvpSummary cancel(UUID actorId, UUID eventId) {
        return submit(actorId, eventId, new SubmitRsvpRequest(RsvpStatus.NO, 0));
    }

    @Transactional(readOnly = true)
    public RsvpSummary mine(UUID actorId, UUID eventId) {
        return rsvps.findByEventIdAndMemberId(eventId, actorId)
                .map(mapper::toSummary)
                .orElseThrow(() -> new NotFoundException("you have not responded to this event"));
    }

    @Transactional(readOnly = true)
    public List<MemberEventSummary> myEvents(UUID actorId) {
        return rsvps.findAllByMemberIdOrderByCreatedAtDesc(actorId).stream()
                .filter(rsvp -> rsvp.getStatus() != RsvpStatus.NO)
                .map(rsvp -> MemberEventSummary.of(
                        eventMapper.toSummary(rsvp.getEvent()), mapper.toSummary(rsvp)))
                .toList();
    }

    /** The attendee list is organizer-only — it is a roster of names and guest counts. */
    @Transactional(readOnly = true)
    public List<AttendeeSummary> attendees(UUID actorId, UUID eventId, RsvpStatus status) {
        Event event = eventService.require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        return rsvps.findAllByEventIdAndStatus(eventId, status).stream()
                .map(mapper::toAttendeeSummary)
                .toList();
    }

    private Event lock(UUID eventId) {
        return events.findByIdForUpdate(eventId).orElseThrow(() -> NotFoundException.of("event", eventId));
    }

    private static void requireRsvpOpen(Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("event is " + event.getStatus() + " and is not taking RSVPs");
        }
        Instant now = Instant.now();
        if (event.getRsvpOpensAt() != null && now.isBefore(event.getRsvpOpensAt())) {
            throw new BusinessRuleException("RSVPs open at " + event.getRsvpOpensAt());
        }
        if (event.getRsvpClosesAt() != null && now.isAfter(event.getRsvpClosesAt())) {
            throw new BusinessRuleException("RSVPs closed at " + event.getRsvpClosesAt());
        }
        if (event.getStartsAt().isBefore(now)) {
            throw new BusinessRuleException("event has already started");
        }
    }

    private static boolean hasRoomFor(Event event, int seats) {
        return event.getCapacity() == null || event.getYesRsvpCount() + seats <= event.getCapacity();
    }

    /** Gives back whatever the existing response was holding, so the new one is priced from scratch. */
    private static void release(Event event, Rsvp rsvp) {
        if (rsvp == null) {
            return;
        }
        if (rsvp.getStatus() == RsvpStatus.YES) {
            event.setYesRsvpCount(Math.max(event.getYesRsvpCount() - (1 + rsvp.getGuestCount()), 0));
        } else if (rsvp.getStatus() == RsvpStatus.WAITLISTED) {
            event.setWaitlistCount(Math.max(event.getWaitlistCount() - 1, 0));
        }
    }

    private Rsvp upsert(Event event, Rsvp existing, UUID actorId, RsvpStatus status, int guestCount) {
        if (existing == null) {
            // The unique (event_id, member_id) index is what actually stops a double-click from
            // creating two rows; this branch just avoids relying on the resulting error.
            return rsvps.save(new Rsvp(event, memberService.require(actorId), status, guestCount));
        }
        existing.setStatus(status);
        existing.setGuestCount(guestCount);
        existing.setRespondedAt(Instant.now());
        return existing;
    }

    /**
     * Walks the waitlist in position order, seating everyone who now fits. Stops at the first
     * party too large for the remaining seats rather than skipping over it — jumping the queue to
     * fill a gap is the kind of surprise that generates support tickets.
     */
    private void promoteFromWaitlist(Event event) {
        if (event.getCapacity() == null) {
            return;
        }
        while (true) {
            Rsvp next = rsvps
                    .findFirstByEventIdAndStatusOrderByWaitlistPositionAsc(event.getId(), RsvpStatus.WAITLISTED)
                    .orElse(null);
            if (next == null || !hasRoomFor(event, 1 + next.getGuestCount())) {
                return;
            }
            next.setStatus(RsvpStatus.YES);
            next.setWaitlistPosition(null);
            next.setPromotedAt(Instant.now());
            event.setYesRsvpCount(event.getYesRsvpCount() + 1 + next.getGuestCount());
            event.setWaitlistCount(Math.max(event.getWaitlistCount() - 1, 0));
            rsvps.flush();
        }
    }
}
