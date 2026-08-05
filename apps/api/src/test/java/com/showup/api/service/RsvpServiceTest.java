package com.showup.api.service;

import com.showup.api.entity.Event;
import com.showup.api.entity.Group;
import com.showup.api.entity.Money;
import com.showup.api.entity.Rsvp;
import com.showup.api.enums.EventFormat;
import com.showup.api.enums.EventStatus;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.dto.SubmitRsvpRequest;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.mapper.EventMapper;
import com.showup.api.mapper.RsvpMapper;
import com.showup.api.repository.EventRepository;
import com.showup.api.repository.RsvpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Seat allocation branches that are inconvenient to reproduce over HTTP: the server-decided
 * waitlist guard, the RSVP time window, and the "stop rather than skip" waitlist-promotion rule.
 * The happy path (seat, waitlist, withdraw, auto-promote) is covered end to end by
 * {@code OrganizerFlowTest}; these are the edges around it.
 */
@ExtendWith(MockitoExtension.class)
class RsvpServiceTest {

    @Mock
    private RsvpRepository rsvps;
    @Mock
    private EventRepository events;
    @Mock
    private EventService eventService;
    @Mock
    private MemberService memberService;
    @Mock
    private GroupAccessGuard guard;
    @Mock
    private RsvpMapper mapper;
    @Mock
    private EventMapper eventMapper;

    private RsvpService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RsvpService(rsvps, events, eventService, memberService, guard, mapper, eventMapper);
    }

    @Test
    void theServerAndNotTheClientDecidesWaitlistPlacement() {
        assertThatThrownBy(() -> service.submit(actorId, eventId, new SubmitRsvpRequest(RsvpStatus.WAITLISTED, 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("waitlist placement is decided by the server");
        verifyNoInteractions(events, rsvps);
    }

    @Test
    void rsvpsAreRefusedBeforeTheOpenWindow() {
        Event event = publishedEvent();
        event.setRsvpOpensAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.submit(actorId, eventId, new SubmitRsvpRequest(RsvpStatus.YES, 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RSVPs open at");
    }

    @Test
    void rsvpsAreRefusedAfterTheCloseWindow() {
        Event event = publishedEvent();
        event.setRsvpClosesAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.submit(actorId, eventId, new SubmitRsvpRequest(RsvpStatus.YES, 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RSVPs closed at");
    }

    @Test
    void aStartedEventNoLongerTakesRsvps() {
        Event event = new Event(mock(Group.class), "Already running", EventFormat.IN_PERSON,
                Instant.now().minus(1, ChronoUnit.HOURS), "UTC", Money.zero("USD"));
        event.setStatus(EventStatus.PUBLISHED);
        when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.submit(actorId, eventId, new SubmitRsvpRequest(RsvpStatus.YES, 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already started");
    }

    @Test
    void guestCountBeyondTheEventsLimitIsRejected() {
        Event event = publishedEvent();
        event.setGuestsPerRsvpLimit(0);
        when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.submit(actorId, eventId, new SubmitRsvpRequest(RsvpStatus.YES, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("guests per RSVP");
    }

    @Test
    void promotionStopsAtATooLargePartyRatherThanSkippingToASmallerOneBehindIt() {
        Event event = publishedEvent();
        event.setCapacity(1);
        event.setYesRsvpCount(1);
        event.setWaitlistEnabled(true);
        event.setGuestsPerRsvpLimit(5);

        Rsvp existing = mock(Rsvp.class);
        when(existing.getStatus()).thenReturn(RsvpStatus.YES);
        when(existing.getGuestCount()).thenReturn(0);

        when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
        when(rsvps.findByEventIdAndMemberId(eventId, actorId)).thenReturn(Optional.of(existing));

        // The head of the waitlist wants two seats; only one is free after this member cancels.
        // The event here is a fresh, unpersisted entity (its id is null), so the stub matches on
        // whatever id promoteFromWaitlist actually looks up by rather than the outer test's eventId.
        Rsvp headOfWaitlist = mock(Rsvp.class);
        when(headOfWaitlist.getGuestCount()).thenReturn(1);
        when(rsvps.findFirstByEventIdAndStatusOrderByWaitlistPositionAsc(any(), eq(RsvpStatus.WAITLISTED)))
                .thenReturn(Optional.of(headOfWaitlist));

        service.cancel(actorId, eventId);

        assertThat(event.getYesRsvpCount()).isZero();
        verify(headOfWaitlist, never()).setStatus(any());
        verify(rsvps, never()).flush();
    }

    private Event publishedEvent() {
        Event event = new Event(mock(Group.class), "Test Event", EventFormat.IN_PERSON,
                Instant.now().plus(30, ChronoUnit.DAYS), "UTC", Money.zero("USD"));
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }
}
