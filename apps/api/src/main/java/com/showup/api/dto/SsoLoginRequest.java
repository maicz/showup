package com.showup.api.dto;

import com.showup.api.enums.IdentityProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SsoLoginRequest(
        @NotNull IdentityProvider provider,
        @NotBlank String idToken) {
}
