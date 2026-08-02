package com.showup.api.member;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}
