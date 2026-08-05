package com.showup.api.controller;

import com.showup.api.dto.EventSummary;
import com.showup.api.dto.GroupSummary;
import com.showup.api.dto.MemberProfile;
import com.showup.api.dto.TopicSummary;
import com.showup.api.dto.UpdateInterestsRequest;
import com.showup.api.dto.UpdateProfileRequest;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.MemberService;
import com.showup.api.service.RsvpService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService members;
    private final RsvpService rsvps;

    MemberController(MemberService members, RsvpService rsvps) {
        this.members = members;
        this.rsvps = rsvps;
    }

    @GetMapping("/me")
    public MemberProfile me(@CurrentMember UUID actor) {
        return members.profile(actor);
    }

    @PutMapping("/me")
    public MemberProfile updateMe(@CurrentMember UUID actor, @Valid @RequestBody UpdateProfileRequest request) {
        return members.updateProfile(actor, request);
    }

    @GetMapping("/{id}")
    public MemberProfile byId(@PathVariable UUID id) {
        return members.profile(id);
    }

    @GetMapping("/me/interests")
    public List<TopicSummary> interests(@CurrentMember UUID actor) {
        return members.interests(actor);
    }

    @PutMapping("/me/interests")
    public List<TopicSummary> updateInterests(@CurrentMember UUID actor,
                                              @Valid @RequestBody UpdateInterestsRequest request) {
        return members.updateInterests(actor, request);
    }

    @GetMapping("/me/groups")
    public List<GroupSummary> groups(@CurrentMember UUID actor) {
        return members.groups(actor);
    }

    /** "My events" — every event the caller is seated or waitlisted for, newest response first. */
    @GetMapping("/me/events")
    public List<EventSummary> events(@CurrentMember UUID actor) {
        return rsvps.myEvents(actor);
    }
}
