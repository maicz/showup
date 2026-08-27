package com.showup.api.service;

import com.showup.api.dto.GroupActivityReport;
import com.showup.api.dto.RsvpRollup;
import com.showup.api.entity.Group;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.repository.CheckInRepository;
import com.showup.api.repository.EventFeedbackRepository;
import com.showup.api.repository.EventRepository;
import com.showup.api.repository.RsvpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private EventRepository events;
    @Mock
    private RsvpRepository rsvps;
    @Mock
    private CheckInRepository checkIns;
    @Mock
    private EventFeedbackRepository feedback;
    @Mock
    private EventService eventService;
    @Mock
    private GroupService groupService;
    @Mock
    private GroupAccessGuard guard;

    private ReportingService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final Instant from = Instant.now().minus(30, ChronoUnit.DAYS);
    private final Instant to = Instant.now();

    @BeforeEach
    void setUp() {
        service = new ReportingService(events, rsvps, checkIns, feedback, eventService, groupService, guard);
    }

    @Test
    void groupActivityRollsUpMetricsViaAggregations() {
        Group group = mock(Group.class);
        when(group.getName()).thenReturn("Tech Meetup");
        when(groupService.require(groupId)).thenReturn(group);

        when(events.countByGroupIdAndStartsAtBetween(groupId, from, to)).thenReturn(5L);
        when(rsvps.aggregateRsvpsForGroupBetween(groupId, from, to, RsvpStatus.YES))
                .thenReturn(new RsvpRollup(20L, 30L));
        when(checkIns.sumAdmittedCountForGroupBetween(groupId, from, to)).thenReturn(25L);
        when(feedback.averageRatingForGroupBetween(groupId, from, to)).thenReturn(4.75);

        GroupActivityReport report = service.groupActivity(actorId, groupId, from, to);

        assertThat(report.groupId()).isEqualTo(groupId);
        assertThat(report.groupName()).isEqualTo("Tech Meetup");
        assertThat(report.eventsHosted()).isEqualTo(5);
        assertThat(report.totalRsvps()).isEqualTo(20);
        assertThat(report.totalCheckIns()).isEqualTo(25);
        assertThat(report.averageAttendanceRate()).isEqualTo(0.8333);
        assertThat(report.averageRating()).isEqualTo(new BigDecimal("4.75"));

        verify(guard).requireEventAdmin(groupId, actorId);
    }

    @Test
    void groupActivityHandlesZeroRegistrationsGracefully() {
        Group group = mock(Group.class);
        when(group.getName()).thenReturn("Empty Meetup");
        when(groupService.require(groupId)).thenReturn(group);

        when(events.countByGroupIdAndStartsAtBetween(groupId, from, to)).thenReturn(0L);
        when(rsvps.aggregateRsvpsForGroupBetween(groupId, from, to, RsvpStatus.YES))
                .thenReturn(new RsvpRollup(0L, 0L));
        when(checkIns.sumAdmittedCountForGroupBetween(groupId, from, to)).thenReturn(0L);
        when(feedback.averageRatingForGroupBetween(groupId, from, to)).thenReturn(null);

        GroupActivityReport report = service.groupActivity(actorId, groupId, from, to);

        assertThat(report.eventsHosted()).isEqualTo(0);
        assertThat(report.totalRsvps()).isEqualTo(0);
        assertThat(report.totalCheckIns()).isEqualTo(0);
        assertThat(report.averageAttendanceRate()).isEqualTo(0.0);
        assertThat(report.averageRating()).isNull();
    }
}
