package com.showup.api.dto;

import java.util.List;

public record AiRecommendationsResponse(
        List<EventSummary> recommendedEvents,
        String rationale) {
}
