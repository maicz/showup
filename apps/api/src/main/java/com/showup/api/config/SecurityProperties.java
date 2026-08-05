package com.showup.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import jakarta.validation.constraints.NotBlank;

import java.time.Duration;

/**
 * Binds {@code showup.security.*}. Typed configuration rather than scattered {@code @Value}
 * lookups, so a missing or too-short signing key fails at startup instead of at the first login.
 */
@ConfigurationProperties(prefix = "showup.security")
public record SecurityProperties(Jwt jwt) {

    public record Jwt(
            @NotBlank String secret,
            @DefaultValue("PT12H") Duration ttl) {

        /** HS256 rejects anything shorter; catching it here names the actual problem. */
        public static final int MIN_SECRET_BYTES = 32;

        public Jwt {
            if (secret != null && secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalStateException(
                        "showup.security.jwt.secret must be at least " + MIN_SECRET_BYTES
                                + " bytes for HS256; set the JWT_SECRET environment variable");
            }
        }
    }
}
