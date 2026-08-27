package com.showup.api.service;

import com.showup.api.dto.EventDetail;
import com.showup.api.dto.RsvpSummary;
import com.showup.api.entity.Event;
import com.showup.api.entity.Group;
import com.showup.api.entity.Rsvp;
import com.showup.api.enums.AvailabilityState;
import com.showup.api.enums.EventFormat;
import com.showup.api.enums.EventStatus;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.mapper.EventMapper;
import com.showup.api.mapper.MemberMapper;
import com.showup.api.mapper.RsvpMapper;
import com.showup.api.repository.EventHostRepository;
import com.showup.api.repository.EventRepository;
import com.showup.api.repository.RsvpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventServiceTest {

    @Mock
    private EventRepository events;
    @Mock
    private EventHostRepository hosts;
    @Mock
    private RsvpRepository rsvps;
    @Mock
    private GroupService groupService;
    @Mock
    private VenueService venueService;
    @Mock
    private MemberService memberService;
    @Mock
    private GroupAccessGuard guard;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private MemberMapper memberMapper;
    @Mock
    private RsvpMapper rsvpMapper;

    private EventService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final UUID viewerId = UUID.randomUUID();
    private final String onlineUrl = "https://meet.google.com/abc-defg-hij";

    @BeforeEach
    void setUp() {
        service = new EventService(
                events, hosts, rsvps, groupService, venueService, memberService,
                guard, eventMapper, memberMapper, rsvpMapper);
    }

    private Event mockEvent() {
        Event event = mock(Event.class);
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(groupId);
        when(event.getGroup()).thenReturn(group);
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(hosts.findAllByEventId(eventId)).thenReturn(List.of());
        return event;
    }

    private EventDetail mockDetail(RsvpSummary viewerRsvp) {
        return new EventDetail(
                eventId, "Online Tech Talk", "Description", EventStatus.PUBLISHED,
                EventFormat.ONLINE, null, onlineUrl, Instant.now(), Instant.now().plusSeconds(3600),
                "UTC", 100, false, 0, null, null, null,
                10, 0, AvailabilityState.SEATS_AVAILABLE, null, List.of(), viewerRsvp);
    }

    @Test
    void detailMasksOnlineUrlForAnonymousViewer() {
        Event event = mockEvent();
        EventDetail raw = mockDetail(null);
        when(eventMapper.toDetail(eq(event), eq(List.of()), eq(null))).thenReturn(raw);

        EventDetail result = service.detail(eventId, Optional.empty());

        assertThat(result.onlineUrl()).isNull();
    }

    @Test
    void detailMasksOnlineUrlForUnseatedViewer() {
        Event event = mockEvent();
        Rsvp rsvp = mock(Rsvp.class);
        RsvpSummary rsvpSummary = new RsvpSummary(UUID.randomUUID(), RsvpStatus.WAITLISTED, 0, 1, Instant.now());
        when(rsvps.findByEventIdAndMemberId(eventId, viewerId)).thenReturn(Optional.of(rsvp));
        when(rsvpMapper.toSummary(rsvp)).thenReturn(rsvpSummary);

        EventDetail raw = mockDetail(rsvpSummary);
        when(eventMapper.toDetail(eq(event), eq(List.of()), eq(rsvpSummary))).thenReturn(raw);
        when(guard.isEventAdmin(groupId, viewerId)).thenReturn(false);
        when(hosts.existsByEventIdAndMemberId(eventId, viewerId)).thenReturn(false);

        EventDetail result = service.detail(eventId, Optional.of(viewerId));

        assertThat(result.onlineUrl()).isNull();
    }

    @Test
    void detailExposesOnlineUrlForConfirmedAttendee() {
        Event event = mockEvent();
        Rsvp rsvp = mock(Rsvp.class);
        RsvpSummary rsvpSummary = new RsvpSummary(UUID.randomUUID(), RsvpStatus.YES, 0, 0, Instant.now());
        when(rsvps.findByEventIdAndMemberId(eventId, viewerId)).thenReturn(Optional.of(rsvp));
        when(rsvpMapper.toSummary(rsvp)).thenReturn(rsvpSummary);

        EventDetail raw = mockDetail(rsvpSummary);
        when(eventMapper.toDetail(eq(event), eq(List.of()), eq(rsvpSummary))).thenReturn(raw);

        EventDetail result = service.detail(eventId, Optional.of(viewerId));

        assertThat(result.onlineUrl()).isEqualTo(onlineUrl);
    }

    @Test
    void detailExposesOnlineUrlForOrganizerEvenWithoutRsvp() {
        Event event = mockEvent();
        when(rsvps.findByEventIdAndMemberId(eventId, viewerId)).thenReturn(Optional.empty());

        EventDetail raw = mockDetail(null);
        when(eventMapper.toDetail(eq(event), eq(List.of()), eq(null))).thenReturn(raw);
        when(guard.isEventAdmin(groupId, viewerId)).thenReturn(true);

        EventDetail result = service.detail(eventId, Optional.of(viewerId));

        assertThat(result.onlineUrl()).isEqualTo(onlineUrl);
    }

    @Test
    void detailExposesOnlineUrlForAssignedHostEvenWithoutRsvp() {
        Event event = mockEvent();
        when(rsvps.findByEventIdAndMemberId(eventId, viewerId)).thenReturn(Optional.empty());

        EventDetail raw = mockDetail(null);
        when(eventMapper.toDetail(eq(event), eq(List.of()), eq(null))).thenReturn(raw);
        when(guard.isEventAdmin(groupId, viewerId)).thenReturn(false);
        when(hosts.existsByEventIdAndMemberId(eventId, viewerId)).thenReturn(true);

        EventDetail result = service.detail(eventId, Optional.of(viewerId));

        assertThat(result.onlineUrl()).isEqualTo(onlineUrl);
    }
}
