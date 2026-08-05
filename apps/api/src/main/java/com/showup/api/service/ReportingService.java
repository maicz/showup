package com.showup.api.service;

import com.showup.api.dto.EventAttendanceReport;
import com.showup.api.dto.GroupActivityReport;
import com.showup.api.entity.CheckIn;
import com.showup.api.entity.Event;
import com.showup.api.entity.Group;
import com.showup.api.entity.Rsvp;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.repository.CheckInRepository;
import com.showup.api.repository.EventFeedbackRepository;
import com.showup.api.repository.EventRepository;
import com.showup.api.repository.RsvpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read models assembled from the operational tables — no entity of their own. Organizer-only:
 * these expose who came, who did not, and how each volunteer performed.
 */
@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final EventRepository events;
    private final RsvpRepository rsvps;
    private final CheckInRepository checkIns;
    private final EventFeedbackRepository feedback;
    private final EventService eventService;
    private final GroupService groupService;
    private final GroupAccessGuard guard;

    ReportingService(EventRepository events, RsvpRepository rsvps, CheckInRepository checkIns,
                     EventFeedbackRepository feedback, EventService eventService,
                     GroupService groupService, GroupAccessGuard guard) {
        this.events = events;
        this.rsvps = rsvps;
        this.checkIns = checkIns;
        this.feedback = feedback;
        this.eventService = eventService;
        this.groupService = groupService;
        this.guard = guard;
    }

    public EventAttendanceReport eventAttendance(UUID actorId, UUID eventId) {
        Event event = eventService.require(eventId);
        guard.requireEventAdmin(event.getGroup().getId(), actorId);

        // Seats, not rows, on both sides: a member who brings two guests is three registrations
        // and, if they all turn up, three admissions. Comparing rows to heads would report a
        // party of three as a single no-show the moment one guest stayed home.
        int registered = rsvps.findAllByEventIdAndStatus(eventId, RsvpStatus.YES).stream()
                .mapToInt(rsvp -> 1 + rsvp.getGuestCount())
                .sum();
        List<CheckIn> scans = checkIns.findAllByTicketRsvpEventId(eventId);
        int attended = scans.stream().mapToInt(CheckIn::getAdmittedCount).sum();

        List<EventAttendanceReport.CheckInTime> times = scans.stream()
                .sorted(Comparator.comparing(CheckIn::getCheckedInAt))
                .map(scan -> new EventAttendanceReport.CheckInTime(scan.getCheckedInAt(), scan.getAdmittedCount()))
                .toList();

        Map<UUID, EventAttendanceReport.StaffScanTotal> byScanner = new LinkedHashMap<>();
        for (CheckIn scan : scans) {
            byScanner.merge(
                    scan.getCheckedInBy().getId(),
                    new EventAttendanceReport.StaffScanTotal(
                            scan.getCheckedInBy().getId(), scan.getCheckedInBy().getDisplayName(), 1),
                    (a, b) -> new EventAttendanceReport.StaffScanTotal(
                            a.memberId(), a.displayName(), a.scanCount() + b.scanCount()));
        }

        return new EventAttendanceReport(
                eventId,
                event.getTitle(),
                registered,
                attended,
                Math.max(registered - attended, 0),
                rate(attended, registered),
                times,
                List.copyOf(byScanner.values()));
    }

    public GroupActivityReport groupActivity(UUID actorId, UUID groupId, Instant from, Instant to) {
        guard.requireEventAdmin(groupId, actorId);
        Group group = groupService.require(groupId);

        List<Event> hosted = events.findAllByGroupIdAndStartsAtBetween(groupId, from, to);
        int totalRsvps = 0;
        int totalRegistered = 0;
        int totalAttended = 0;
        for (Event event : hosted) {
            List<Rsvp> yes = rsvps.findAllByEventIdAndStatus(event.getId(), RsvpStatus.YES);
            totalRsvps += yes.size();
            totalRegistered += yes.stream().mapToInt(rsvp -> 1 + rsvp.getGuestCount()).sum();
            totalAttended += checkIns.findAllByTicketRsvpEventId(event.getId()).stream()
                    .mapToInt(CheckIn::getAdmittedCount)
                    .sum();
        }
        Double averageRating = feedback.averageRatingForGroupBetween(groupId, from, to);

        return new GroupActivityReport(
                groupId,
                group.getName(),
                from,
                to,
                hosted.size(),
                totalRsvps,
                totalAttended,
                rate(totalAttended, totalRegistered),
                averageRating == null ? null : BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP));
    }

    /** Zero registrations means an undefined rate, reported as 0 rather than NaN. */
    private static double rate(int attended, int registered) {
        return registered == 0 ? 0d
                : BigDecimal.valueOf((double) attended / registered)
                        .setScale(4, RoundingMode.HALF_UP)
                        .doubleValue();
    }
}
