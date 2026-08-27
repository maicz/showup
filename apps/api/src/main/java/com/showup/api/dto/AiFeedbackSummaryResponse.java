package com.showup.api.dto;

import java.util.List;

public record AiFeedbackSummaryResponse(
        String overallSentiment,
        double averageRating,
        List<String> topThemes,
        List<String> positiveHighlights,
        List<String> improvementSuggestions,
        String narrativeSummary) {
}
