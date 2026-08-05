package com.showup.api.controller;

import com.showup.api.dto.VenueSummary;
import com.showup.api.service.VenueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Single-venue lookup. Creating and listing a group's venues lives under
 * {@code /api/groups/{id}/venues} in {@link GroupController}, because a venue is owned by the
 * group that entered it.
 */
@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venues;

    VenueController(VenueService venues) {
        this.venues = venues;
    }

    @GetMapping("/{id}")
    public VenueSummary byId(@PathVariable UUID id) {
        return venues.get(id);
    }
}
