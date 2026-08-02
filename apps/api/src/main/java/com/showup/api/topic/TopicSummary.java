package com.showup.api.topic;

import java.util.UUID;

public record TopicSummary(
        UUID id,
        String slug,
        String name,
        String categorySlug,
        int groupCount) {
}
