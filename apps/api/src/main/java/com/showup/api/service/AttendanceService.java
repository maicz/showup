package com.showup.api.service;

import com.showup.api.dto.AssignStaffRequest;
import com.showup.api.dto.CheckInRequest;
import com.showup.api.dto.CheckInResponse;
import com.showup.api.dto.StaffAssignmentSummary;
import com.showup.api.dto.TicketResponse;
import com.showup.api.entity.CheckIn;
import com.showup.api.entity.Event;
import com.showup.api.entity.Rsvp;
import com.showup.api.entity.StaffAssignment;
import com.showup.api.entity.Ticket;
import com.showup.api.enums.EventStatus;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.enums.StaffRole;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ConflictException;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.AttendanceMapper;
import com.showup.api.repository.CheckInRepository;
import com.showup.api.repository.RsvpRepository;
import com.showup.api.repository.StaffAssignmentRepository;
import com.showup.api.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Tickets, door scanning, and staffing — the operational half of an event. */
@Service
@Transactional
public class AttendanceService {

    private final TicketRepository tickets;
    private final CheckInRepository checkIns;
    private final StaffAssignmentRepository staff;
    private final RsvpRepository rsvps;
    private final EventService eventService;
    private final MemberService memberService;
    private final GroupAccessGuard guard;
    private final AttendanceMapper mapper;

    AttendanceService(TicketRepository tickets, CheckInRepository checkIns, StaffAssignmentRepository staff,
                      RsvpRepository rsvps, EventService eventService, MemberService memberService,
                      GroupAccessGuard guard, AttendanceMapper mapper) {
        this.tickets = tickets;
        this.checkIns = checkIns;
        this.staff = staff;
        this.rsvps = rsvps;
        this.eventService = eventService;
        this.memberService = memberService;
        this.guard = guard;
        this.mapper = mapper;
    }

    /** Idempotent: asking twice returns the same ticket rather than invalidating the first QR code. */
    public TicketResponse issueTicket(UUID actorId, UUID eventId) {
        Rsvp rsvp = rsvps.findByEventIdAndMemberId(eventId, actorId)
                .orElseThrow(() -> new NotFoundException("you have not RSVPed to this event"));
        if (rsvp.getStatus() != RsvpStatus.YES) {
            throw new BusinessRuleException("a ticket needs a seated RSVP; yours is " + rsvp.getStatus());
        }
        Ticket ticket = tickets.findByRsvpId(rsvp.getId())
                .orElseGet(() -> tickets.save(new Ticket(rsvp, 1 + rsvp.getGuestCount())));
        // Guests can change after the ticket is issued; the door needs the current number.
        ticket.setAdmitCount(1 + rsvp.getGuestCount());
        return mapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse myTicket(UUID actorId, UUID eventId) {
        Rsvp rsvp = rsvps.findByEventIdAndMemberId(eventId, actorId)
                .orElseThrow(() -> new NotFoundException("you have not RSVPed to this event"));
        return tickets.findByRsvpId(rsvp.getId())
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("no ticket has been issued for this RSVP"));
    }

    /**
     * The door. Idempotent by construction — {@code check_in.ticket_id} is unique, so a second
     * scan of the same code returns the first check-in instead of double-counting a head.
     */
    public CheckInResponse checkIn(UUID actorId, UUID eventId, CheckInRequest request) {
        Event event = eventService.require(eventId);
        requireScanner(actorId, event);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("event is cancelled");
        }

        Ticket ticket = tickets.findByCode(request.ticketCode())
                .orElseThrow(() -> NotFoundException.of("ticket", request.ticketCode()));
        if (!ticket.getRsvp().getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException("that ticket belongs to a different event");
        }
        if (ticket.getRevokedAt() != null) {
            throw new BusinessRuleException("ticket was revoked at " + ticket.getRevokedAt());
        }
        if (request.admittedCount() > ticket.getAdmitCount()) {
            throw new BusinessRuleException("ticket admits " + ticket.getAdmitCount()
                    + "; cannot admit " + request.admittedCount());
        }

        CheckIn existing = checkIns.findByTicketId(ticket.getId()).orElse(null);
        if (existing != null) {
            return mapper.toResponse(existing);
        }
        CheckIn checkIn = new CheckIn(ticket, memberService.require(actorId),
                request.method(), request.admittedCount());
        return mapper.toResponse(checkIns.save(checkIn));
    }

    public TicketResponse revokeTicket(UUID actorId, UUID eventId, UUID ticketId) {
        Event event = eventService.require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> NotFoundException.of("ticket", ticketId));
        if (!ticket.getRsvp().getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException("that ticket belongs to a different event");
        }
        ticket.setRevokedAt(Instant.now());
        return mapper.toResponse(ticket);
    }

    public StaffAssignmentSummary assignStaff(UUID actorId, UUID eventId, AssignStaffRequest request) {
        Event event = eventService.require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        // Staff run the event on the group's behalf, so they have to belong to the group.
        guard.requireActiveMember(event.getGroup().getId(), request.memberId());
        if (staff.existsByEventIdAndMemberIdAndRole(eventId, request.memberId(), request.role())) {
            throw new ConflictException("that member already holds the " + request.role() + " role here");
        }
        StaffAssignment assignment = new StaffAssignment(event, memberService.require(request.memberId()),
                request.role(), request.shiftStartsAt(), request.shiftEndsAt());
        assignment.setNotes(request.notes());
        return mapper.toSummary(staff.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<StaffAssignmentSummary> staff(UUID actorId, UUID eventId) {
        Event event = eventService.require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        return staff.findAllByEventId(eventId).stream().map(mapper::toSummary).toList();
    }

    public void removeStaff(UUID actorId, UUID eventId, UUID assignmentId) {
        Event event = eventService.require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);
        StaffAssignment assignment = staff.findById(assignmentId)
                .orElseThrow(() -> NotFoundException.of("staff assignment", assignmentId));
        if (!assignment.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException("that assignment belongs to a different event");
        }
        staff.delete(assignment);
    }

    /**
     * Scanning is the one action delegated outside the organizer roles: a volunteer assigned
     * {@link StaffRole#SCANNER} can work the door without being given rights over the group.
     */
    private void requireScanner(UUID actorId, Event event) {
        boolean organizer = guard.isEventAdmin(event.getGroup().getId(), actorId);
        boolean scanner = staff.existsByEventIdAndMemberIdAndRole(event.getId(), actorId, StaffRole.SCANNER);
        if (!organizer && !scanner) {
            throw new ForbiddenException("check-in requires an organizer role or a SCANNER assignment");
        }
    }
}
