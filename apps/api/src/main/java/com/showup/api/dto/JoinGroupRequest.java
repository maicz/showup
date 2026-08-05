package com.showup.api.dto;

import jakarta.validation.constraints.Size;

public record JoinGroupRequest(
        @Size(max = 5_000) String introduction) {
}
