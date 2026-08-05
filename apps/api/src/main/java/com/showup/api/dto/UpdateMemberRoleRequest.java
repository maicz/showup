package com.showup.api.dto;

import com.showup.api.enums.GroupMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull GroupMemberRole role) {
}
