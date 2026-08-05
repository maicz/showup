package com.showup.api.controller;

import com.showup.api.dto.AssignStaffRequest;
import com.showup.api.dto.CheckInRequest;
import com.showup.api.dto.CheckInResponse;
import com.showup.api.dto.StaffAssignmentSummary;
import com.showup.api.dto.TicketResponse;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}")
public class AttendanceController {

    private final AttendanceService attendance;

    AttendanceController(AttendanceService attendance) {
        this.attendance = attendance;
    }

    @PostMapping("/ticket")
    public TicketResponse issueTicket(@CurrentMember UUID actor, @PathVariable UUID eventId) {
        return attendance.issueTicket(actor, eventId);
    }

    @GetMapping("/ticket")
    public TicketResponse myTicket(@CurrentMember UUID actor, @PathVariable UUID eventId) {
        return attendance.myTicket(actor, eventId);
    }

    /** The door. Safe to call twice with the same code — the second scan returns the first result. */
    @PostMapping("/check-ins")
    public CheckInResponse checkIn(@CurrentMember UUID actor, @PathVariable UUID eventId,
                                   @Valid @RequestBody CheckInRequest request) {
        return attendance.checkIn(actor, eventId, request);
    }

    @PostMapping("/tickets/{ticketId}/revoke")
    public TicketResponse revoke(@CurrentMember UUID actor, @PathVariable UUID eventId,
                                 @PathVariable UUID ticketId) {
        return attendance.revokeTicket(actor, eventId, ticketId);
    }

    @GetMapping("/staff")
    public List<StaffAssignmentSummary> staff(@CurrentMember UUID actor, @PathVariable UUID eventId) {
        return attendance.staff(actor, eventId);
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffAssignmentSummary assignStaff(@CurrentMember UUID actor, @PathVariable UUID eventId,
                                              @Valid @RequestBody AssignStaffRequest request) {
        return attendance.assignStaff(actor, eventId, request);
    }

    @DeleteMapping("/staff/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStaff(@CurrentMember UUID actor, @PathVariable UUID eventId,
                            @PathVariable UUID assignmentId) {
        attendance.removeStaff(actor, eventId, assignmentId);
    }
}
