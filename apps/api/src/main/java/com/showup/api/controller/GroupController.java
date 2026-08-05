package com.showup.api.controller;

import com.showup.api.dto.CreateGroupRequest;
import com.showup.api.dto.CreateVenueRequest;
import com.showup.api.dto.EventSummary;
import com.showup.api.dto.GroupDetail;
import com.showup.api.dto.GroupMemberSummary;
import com.showup.api.dto.GroupSummary;
import com.showup.api.dto.JoinGroupRequest;
import com.showup.api.dto.PageResponse;
import com.showup.api.dto.UpdateGroupRequest;
import com.showup.api.dto.UpdateMemberRoleRequest;
import com.showup.api.dto.VenueSummary;
import com.showup.api.enums.GroupMembershipStatus;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.EventService;
import com.showup.api.service.GroupService;
import com.showup.api.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groups;
    private final VenueService venues;
    private final EventService events;

    GroupController(GroupService groups, VenueService venues, EventService events) {
        this.groups = groups;
        this.venues = venues;
        this.events = events;
    }

    @GetMapping
    public PageResponse<GroupSummary> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return groups.list(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDetail create(@CurrentMember UUID actor, @Valid @RequestBody CreateGroupRequest request) {
        return groups.create(actor, request);
    }

    @GetMapping("/{id}")
    public GroupDetail byId(@PathVariable UUID id) {
        return groups.detail(id);
    }

    @GetMapping("/by-urlname/{urlname}")
    public GroupDetail byUrlname(@PathVariable String urlname) {
        return groups.detailByUrlname(urlname);
    }

    @PutMapping("/{id}")
    public GroupDetail update(@CurrentMember UUID actor, @PathVariable UUID id,
                              @Valid @RequestBody UpdateGroupRequest request) {
        return groups.update(actor, id, request);
    }

    // --- membership ---

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberSummary join(@CurrentMember UUID actor, @PathVariable UUID id,
                                   @Valid @RequestBody JoinGroupRequest request) {
        return groups.join(actor, id, request);
    }

    @DeleteMapping("/{id}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@CurrentMember UUID actor, @PathVariable UUID id) {
        groups.leave(actor, id);
    }

    @GetMapping("/{id}/members")
    public List<GroupMemberSummary> members(@CurrentMember UUID actor, @PathVariable UUID id,
                                            @RequestParam(defaultValue = "ACTIVE") GroupMembershipStatus status) {
        return groups.members(actor, id, status);
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public GroupMemberSummary updateRole(@CurrentMember UUID actor, @PathVariable UUID id,
                                         @PathVariable UUID memberId,
                                         @Valid @RequestBody UpdateMemberRoleRequest request) {
        return groups.updateMemberRole(actor, id, memberId, request);
    }

    @PostMapping("/{id}/members/{memberId}/approve")
    public GroupMemberSummary approve(@CurrentMember UUID actor, @PathVariable UUID id,
                                      @PathVariable UUID memberId) {
        return groups.approveMember(actor, id, memberId);
    }

    // --- owned resources ---

    @GetMapping("/{id}/venues")
    public List<VenueSummary> venues(@CurrentMember UUID actor, @PathVariable UUID id) {
        return venues.listByGroup(actor, id);
    }

    @PostMapping("/{id}/venues")
    @ResponseStatus(HttpStatus.CREATED)
    public VenueSummary createVenue(@CurrentMember UUID actor, @PathVariable UUID id,
                                    @Valid @RequestBody CreateVenueRequest request) {
        return venues.create(actor, id, request);
    }

    @GetMapping("/{id}/events")
    public List<EventSummary> events(@PathVariable UUID id) {
        return events.byGroup(id);
    }
}
