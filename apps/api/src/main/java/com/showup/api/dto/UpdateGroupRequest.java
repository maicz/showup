package com.showup.api.dto;

import com.showup.api.enums.GroupJoinPolicy;
import com.showup.api.enums.GroupVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateGroupRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 20_000) String description,
        @NotNull UUID categoryId,
        String city,
        String country,
        GeoPoint location,
        @NotBlank String timeZone,
        @NotNull GroupVisibility visibility,
        @NotNull GroupJoinPolicy joinPolicy,
        List<UUID> topicIds) {
}
