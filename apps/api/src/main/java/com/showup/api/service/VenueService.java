package com.showup.api.service;

import com.showup.api.dto.CreateVenueRequest;
import com.showup.api.dto.VenueSummary;
import com.showup.api.entity.Address;
import com.showup.api.entity.Venue;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.VenueMapper;
import com.showup.api.repository.VenueRepository;
import com.showup.api.util.GeoPoints;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Venues are owned by the group that created them, so a regular space is entered once. */
@Service
@Transactional
public class VenueService {

    private final VenueRepository venues;
    private final GroupService groupService;
    private final GroupAccessGuard guard;
    private final VenueMapper mapper;

    VenueService(VenueRepository venues, GroupService groupService, GroupAccessGuard guard, VenueMapper mapper) {
        this.venues = venues;
        this.groupService = groupService;
        this.guard = guard;
        this.mapper = mapper;
    }

    public VenueSummary create(UUID actorId, UUID groupId, CreateVenueRequest request) {
        guard.requireEventAdmin(groupId, actorId);
        Address address = new Address(request.addressLine1(), request.addressLine2(), request.city(),
                request.region(), request.postalCode(), request.country());
        Venue venue = new Venue(request.name(), address, GeoPoints.toJts(request.location()),
                request.notes(), groupService.require(groupId));
        return mapper.toSummary(venues.save(venue));
    }

    @Transactional(readOnly = true)
    public List<VenueSummary> listByGroup(UUID actorId, UUID groupId) {
        guard.requireActiveMember(groupId, actorId);
        return venues.findAllByCreatedByGroupId(groupId).stream().map(mapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public VenueSummary get(UUID venueId) {
        return mapper.toSummary(require(venueId));
    }

    Venue require(UUID venueId) {
        return venues.findById(venueId).orElseThrow(() -> NotFoundException.of("venue", venueId));
    }
}
