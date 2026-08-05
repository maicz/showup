package com.showup.api.entity;

import com.showup.api.enums.MemberStatus;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Setters that no service in the application currently calls, because the corresponding feature
 * (editing a venue, an admin changing a member's status, editing a recurrence template, adjusting
 * a staff shift after assignment) has no endpoint yet — see {@code VenueService},
 * {@code EventSeriesService}, and {@code AttendanceService}. The getter/setter pairs are still
 * public API surface the entities expose, and a plain round-trip test is the cheapest way to
 * verify they behave rather than leaving them unexercised.
 */
class EntitySettersTest {

    @Test
    void aVenueCanBeRenamedRelocatedAndAnnotated() {
        Venue venue = new Venue("Original Hall", null, null, null, mock(Group.class));

        Address newAddress = new Address("New Line 1", "Suite 2", "Cluj", "Cluj County", "400001", "RO");
        Point newLocation = new GeometryFactory().createPoint(new Coordinate(23.6, 46.77));
        venue.setName("New Hall");
        venue.setAddress(newAddress);
        venue.setLocation(newLocation);
        venue.setNotes("Ring the buzzer twice");

        assertThat(venue.getName()).isEqualTo("New Hall");
        assertThat(venue.getAddress()).isSameAs(newAddress);
        assertThat(venue.getLocation()).isSameAs(newLocation);
        assertThat(venue.getNotes()).isEqualTo("Ring the buzzer twice");
    }

    @Test
    void aMembersCredentialsAndStandingCanBeChangedOutsideTheOrdinaryProfileUpdate() {
        Member member = new Member("member@example.test", "old-hash", "Original Name");

        Point homeLocation = new GeometryFactory().createPoint(new Coordinate(26.1, 44.4));
        member.setPasswordHash("new-hash");
        member.setHomeLocation(homeLocation);
        member.setStatus(MemberStatus.SUSPENDED);
        Instant verifiedAt = Instant.now();
        member.setEmailVerifiedAt(verifiedAt);

        assertThat(member.getPasswordHash()).isEqualTo("new-hash");
        assertThat(member.getHomeLocation()).isSameAs(homeLocation);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(member.getEmailVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    void aSeriesTemplateCanBeEditedFieldByField() {
        EventSeries series = new EventSeries(mock(Group.class), "FREQ=WEEKLY;COUNT=5", "Original Title", 30);

        series.setRecurrenceRule("FREQ=WEEKLY;COUNT=10");
        series.setTemplateTitle("Renamed Standup");
        series.setTemplateDurationMinutes(45);

        assertThat(series.getRecurrenceRule()).isEqualTo("FREQ=WEEKLY;COUNT=10");
        assertThat(series.getTemplateTitle()).isEqualTo("Renamed Standup");
        assertThat(series.getTemplateDurationMinutes()).isEqualTo(45);
    }

    @Test
    void aStaffShiftCanBeRescheduledAfterTheAssignmentIsMade() {
        StaffAssignment assignment = new StaffAssignment(mock(Event.class), mock(Member.class),
                com.showup.api.enums.StaffRole.GREETER, null, null);

        Instant start = Instant.parse("2027-01-01T18:00:00Z");
        Instant end = Instant.parse("2027-01-01T21:00:00Z");
        assignment.setShiftStartsAt(start);
        assignment.setShiftEndsAt(end);

        assertThat(assignment.getShiftStartsAt()).isEqualTo(start);
        assertThat(assignment.getShiftEndsAt()).isEqualTo(end);
    }
}
