package com.showup.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpdateInterestsRequest(
        @NotNull List<UUID> topicIds) {
}
