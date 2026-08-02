package com.showup.api.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 80) String displayName,
        @Size(max = 20_000) String bio,
        @Size(max = 500) String photoUrl,
        String homeCity,
        String homeCountry) {
}
