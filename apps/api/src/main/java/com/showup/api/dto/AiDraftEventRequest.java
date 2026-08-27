package com.showup.api.dto;

import com.showup.api.enums.EventFormat;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AiDraftEventRequest(
        @NotBlank String prompt,
        String groupContext,
        String topicPreference) {
}
