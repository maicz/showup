package com.showup.api.dto;

import com.showup.api.enums.AvailabilityState;
import com.showup.api.enums.EventFormat;

import java.time.Instant;
import java.util.List;

/**
 * Binds the facets the product actually filters on. See docs/domain-model.md#requests.
 *
 * <p>Every facet is optional, so every component is a reference type — including {@code page} and
 * {@code size}. A primitive {@code int} would make them mandatory in practice: an absent query
 * parameter binds as null, and null does not convert to {@code int}, so {@code /events/search}
 * with no paging parameters would fail with a 400 instead of returning the first page.
 *
 * <p>The compact constructor normalizes paging once, here, so no downstream caller has to repeat
 * the null-and-range checks.
 */
public record EventSearchQuery(
        Double lat,
        Double lon,
        Double radiusKm,
        String categorySlug,
        List<String> topicSlugs,
        EventFormat format,
        Instant dateFrom,
        Instant dateTo,
        Long maxFee,
        AvailabilityState availability,
        String query,
        Integer page,
        Integer size,
        String sort) {

    public static final int DEFAULT_SIZE = 20;

    /** Caps how much one caller can pull in a single request. */
    public static final int MAX_SIZE = 100;

    public EventSearchQuery {
        query = query == null || query.isBlank() ? null : query.trim().toLowerCase();
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
