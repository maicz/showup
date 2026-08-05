package com.showup.api.dto;

import com.showup.api.enums.GroupJoinPolicy;
import com.showup.api.enums.GroupStatus;
import com.showup.api.enums.GroupVisibility;

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
