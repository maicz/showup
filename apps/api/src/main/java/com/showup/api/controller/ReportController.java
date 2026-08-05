package com.showup.api.controller;

import com.showup.api.dto.EventAttendanceReport;
import com.showup.api.dto.GroupActivityReport;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.ReportingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** Organizer-only read models. See docs/domain-model.md#full-dto-catalog. */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportingService reporting;

    ReportController(ReportingService reporting) {
        this.reporting = reporting;
    }

    @GetMapping("/events/{eventId}/reports/attendance")
    public EventAttendanceReport eventAttendance(@CurrentMember UUID actor, @PathVariable UUID eventId) {
        return reporting.eventAttendance(actor, eventId);
    }

    @GetMapping("/groups/{groupId}/reports/activity")
    public GroupActivityReport groupActivity(@CurrentMember UUID actor, @PathVariable UUID groupId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                             Instant from,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                             Instant to) {
        return reporting.groupActivity(actor, groupId, from, to);
    }
}
