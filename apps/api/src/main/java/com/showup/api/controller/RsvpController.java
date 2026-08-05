package com.showup.api.controller;

import com.showup.api.dto.AttendeeSummary;
import com.showup.api.dto.RsvpSummary;
import com.showup.api.dto.SubmitRsvpRequest;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.RsvpService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}")
public class RsvpController {

    private final RsvpService rsvps;

    RsvpController(RsvpService rsvps) {
        this.rsvps = rsvps;
    }

    /** PUT, not POST: one member has at most one RSVP per event, and re-answering replaces it. */
    @PutMapping("/rsvp")
    public RsvpSummary submit(@CurrentMember UUID actor, @PathVariable UUID eventId,
                              @Valid @RequestBody SubmitRsvpRequest request) {
        return rsvps.submit(actor, eventId, request);
    }

    @GetMapping("/rsvp")
    public RsvpSummary mine(@CurrentMember UUID actor, @PathVariable UUID eventId) {
        return rsvps.mine(actor, eventId);
    }

    @DeleteMapping("/rsvp")
    public RsvpSummary cancel(@CurrentMember UUID actor, @PathVariable UUID eventId) {
        return rsvps.cancel(actor, eventId);
    }

    /** Organizer-only: a roster of names and guest counts. */
    @GetMapping("/attendees")
    public List<AttendeeSummary> attendees(@CurrentMember UUID actor, @PathVariable UUID eventId,
                                           @RequestParam(defaultValue = "YES") RsvpStatus status) {
        return rsvps.attendees(actor, eventId, status);
    }
}
