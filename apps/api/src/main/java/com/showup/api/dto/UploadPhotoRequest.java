package com.showup.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UploadPhotoRequest(
        @NotBlank @Size(max = 500) String url,
        @Size(max = 500) String caption,
        @Positive int width,
        @Positive int height) {
}
