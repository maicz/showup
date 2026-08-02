package com.showup.api.group;

import jakarta.validation.constraints.Size;

public record JoinGroupRequest(
        @Size(max = 5_000) String introduction) {
}
