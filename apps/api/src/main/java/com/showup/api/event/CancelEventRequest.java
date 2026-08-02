package com.showup.api.event;

import jakarta.validation.constraints.Size;

public record CancelEventRequest(
        @Size(max = 2_000) String reason) {
}
