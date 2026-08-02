package com.showup.api.group;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull GroupMemberRole role) {
}
