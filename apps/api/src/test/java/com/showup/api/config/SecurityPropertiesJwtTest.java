package com.showup.api.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SecurityProperties.Jwt}'s compact constructor is what turns a too-short signing key into
 * a startup failure naming the actual problem, rather than a mysterious HS256 error on first
 * login. The running application only ever exercises the "long enough" branch; this fills in the
 * other two without needing to boot Spring at all.
 */
class SecurityPropertiesJwtTest {

    @Test
    void aSecretShorterThanThirtyTwoBytesFailsFast() {
        assertThatThrownBy(() -> new SecurityProperties.Jwt("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void aSecretOfExactlyThirtyTwoBytesIsAccepted() {
        assertThatCode(() -> new SecurityProperties.Jwt("x".repeat(32), Duration.ofHours(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void aNullSecretIsNotValidatedHereBeanValidationCatchesItInstead() {
        assertThatCode(() -> new SecurityProperties.Jwt(null, Duration.ofHours(1)))
                .doesNotThrowAnyException();
    }
}
