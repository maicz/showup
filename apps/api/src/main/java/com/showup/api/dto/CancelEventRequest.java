package com.showup.api.dto;

import jakarta.validation.constraints.Size;

public record CancelEventRequest(
        @Size(max = 2_000) String reason) {
}
