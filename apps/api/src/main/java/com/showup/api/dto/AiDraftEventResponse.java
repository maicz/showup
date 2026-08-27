package com.showup.api.dto;

import com.showup.api.enums.EventFormat;

import java.util.List;

public record AiDraftEventResponse(
        String title,
        String description,
        EventFormat format,
        String suggestedCategorySlug,
        List<String> suggestedTopicSlugs,
        Integer estimatedCapacity,
        Integer durationMinutes) {
}
