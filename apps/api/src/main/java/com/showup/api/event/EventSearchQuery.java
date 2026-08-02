package com.showup.api.event;

import java.time.Instant;
import java.util.List;

/**
 * Binds the facets the product actually filters on. See docs/domain-model.md#requests.
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
        int page,
        int size,
        String sort) {
}
