package com.showup.api.group;

import com.showup.api.topic.CategorySummary;

import java.math.BigDecimal;
import java.util.UUID;

public record GroupSummary(
        UUID id,
        String urlname,
        String name,
        CategorySummary category,
        String city,
        String country,
        int memberCount,
        BigDecimal ratingAverage,
        int ratingCount) {
}
