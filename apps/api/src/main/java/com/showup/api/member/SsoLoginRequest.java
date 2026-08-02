package com.showup.api.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SsoLoginRequest(
        @NotNull IdentityProvider provider,
        @NotBlank String idToken) {
}
