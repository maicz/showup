package com.showup.api.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}
