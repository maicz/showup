package com.showup.api.dto;

import java.util.UUID;

public record TopicSummary(
        UUID id,
        String slug,
        String name,
        String categorySlug,
        int groupCount) {
}
