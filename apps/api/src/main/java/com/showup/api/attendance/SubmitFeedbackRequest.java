package com.showup.api.attendance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SubmitFeedbackRequest(
        @Min(1) @Max(5) int rating,
        @Size(max = 5_000) String comment) {
}
