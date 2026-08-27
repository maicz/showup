package com.showup.api.service;

import com.showup.api.entity.Event;
import com.showup.api.entity.Group;
import com.showup.api.entity.Rsvp;
import com.showup.api.entity.Ticket;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.enums.StaffRole;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.AttendanceMapper;
import com.showup.api.repository.CheckInRepository;
import com.showup.api.repository.RsvpRepository;
import com.showup.api.repository.StaffAssignmentRepository;
import com.showup.api.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Branches of the door/staffing workflow that a single HTTP round trip does not reach: ticket
 * issuance guarded by RSVP status, idempotent re-issuance, and the "organizer or assigned
 * SCANNER" authorization rule in isolation from the rest of check-in.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private TicketRepository tickets;
    @Mock
    private CheckInRepository checkIns;
    @Mock
    private StaffAssignmentRepository staff;
    @Mock
    private RsvpRepository rsvps;
    @Mock
    private EventService eventService;
    @Mock
    private MemberService memberService;
    @Mock
    private GroupAccessGuard guard;
    @Mock
    private AttendanceMapper mapper;

    private AttendanceService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AttendanceService(tickets, checkIns, staff, rsvps, eventService, memberService, guard, mapper);
    }

    @Test
    void aTicketCannotBeIssuedForAWaitlistedRsvp() {
        Rsvp rsvp = mock(Rsvp.class);
        when(rsvp.getStatus()).thenReturn(RsvpStatus.WAITLISTED);
        when(rsvps.findByEventIdAndMemberId(eventId, actorId)).thenReturn(Optional.of(rsvp));

        assertThatThrownBy(() -> service.issueTicket(actorId, eventId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("seated RSVP");
    }

    @Test
    void issuingATicketTwiceReturnsTheSameOneWithARefreshedAdmitCount() {
        Rsvp rsvp = mock(Rsvp.class);
        UUID rsvpId = UUID.randomUUID();
        when(rsvp.getStatus()).thenReturn(RsvpStatus.YES);
        when(rsvp.getId()).thenReturn(rsvpId);
        when(rsvp.getGuestCount()).thenReturn(2);
        when(rsvps.findByEventIdAndMemberId(eventId, actorId)).thenReturn(Optional.of(rsvp));

        Ticket existing = mock(Ticket.class);
        when(tickets.findByRsvpId(rsvp.getId())).thenReturn(Optional.of(existing));
        UUID existingTicketId = UUID.randomUUID();
        when(existing.getId()).thenReturn(existingTicketId);
        when(mapper.toResponse(existing)).thenReturn(
                new com.showup.api.dto.TicketResponse(existingTicketId, "code", 3, java.time.Instant.now(), false, false));

        service.issueTicket(actorId, eventId);

        // A guest was added since the ticket was first issued; admitCount must reflect it now.
        verify(existing).setAdmitCount(3);
        verify(tickets, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void myTicketFailsWithoutAnRsvpAtAllAndAlsoWithoutAnIssuedTicket() {
        when(rsvps.findByEventIdAndMemberId(eventId, actorId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.myTicket(actorId, eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not RSVPed");

        Rsvp rsvp = mock(Rsvp.class);
        when(rsvp.getId()).thenReturn(UUID.randomUUID());
        when(rsvps.findByEventIdAndMemberId(eventId, actorId)).thenReturn(Optional.of(rsvp));
        when(tickets.findByRsvpId(rsvp.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.myTicket(actorId, eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("no ticket has been issued");
    }

    @Test
    void scanningRequiresEitherAnOrganizerRoleOrAScannerAssignmentNotBoth() {
        UUID groupId = UUID.randomUUID();
        Event event = mock(Event.class);
        Group group = mock(Group.class);
        when(event.getId()).thenReturn(eventId);
        when(event.getGroup()).thenReturn(group);
        when(group.getId()).thenReturn(groupId);
        when(eventService.require(eventId)).thenReturn(event);
        when(guard.isEventAdmin(groupId, actorId)).thenReturn(false);
        when(staff.existsByEventIdAndMemberIdAndRole(eventId, actorId, StaffRole.SCANNER)).thenReturn(false);

        assertThatThrownBy(() -> service.checkIn(actorId, eventId,
                new com.showup.api.dto.CheckInRequest("whatever", com.showup.api.enums.CheckInMethod.MANUAL, 1)))
                .isInstanceOf(com.showup.api.exception.ForbiddenException.class);

        // The guard runs before any ticket lookup at all.
        verify(tickets, never()).findByCode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void anAssignmentBelongingToAnotherEventCannotBeRemovedFromThisOne() {
        Event event = new Event(mock(Group.class), "Door", com.showup.api.enums.EventFormat.IN_PERSON,
                java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS), "UTC",
                com.showup.api.entity.Money.zero("USD"));
        when(eventService.require(eventId)).thenReturn(event);

        com.showup.api.entity.StaffAssignment assignment = mock(com.showup.api.entity.StaffAssignment.class);
        Event otherEvent = mock(Event.class);
        when(otherEvent.getId()).thenReturn(UUID.randomUUID());
        when(assignment.getEvent()).thenReturn(otherEvent);
        UUID assignmentId = UUID.randomUUID();
        when(staff.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.removeStaff(actorId, eventId, assignmentId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different event");
        verify(staff, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
