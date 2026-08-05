package com.showup.api.dto;

import com.showup.api.enums.MemberStatus;

import java.time.Instant;
import java.util.UUID;

public record MemberProfile(
        UUID id,
        String email,
        String displayName,
        String bio,
        String photoUrl,
        String homeCity,
        String homeCountry,
        MemberStatus status,
        boolean emailVerified,
        Instant memberSince) {
}
