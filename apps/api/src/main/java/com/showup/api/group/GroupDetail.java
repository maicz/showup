package com.showup.api.group;

import com.showup.api.shared.GeoPoint;
import com.showup.api.topic.CategorySummary;
import com.showup.api.topic.TopicSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupDetail(
        UUID id,
        String urlname,
        String name,
        String description,
        CategorySummary category,
        String city,
        String country,
        GeoPoint location,
        String timeZone,
        GroupVisibility visibility,
        GroupJoinPolicy joinPolicy,
        int memberCount,
        BigDecimal ratingAverage,
        int ratingCount,
        Instant foundedAt,
        GroupStatus status,
        List<TopicSummary> topics) {
}
