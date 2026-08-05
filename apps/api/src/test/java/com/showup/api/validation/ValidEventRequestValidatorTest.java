package com.showup.api.validation;

import com.showup.api.dto.EventLocationAndSchedule;
import com.showup.api.enums.EventFormat;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every branch of the cross-field constraint, in isolation from bean validation's plumbing.
 * {@code OrganizerFlowTest} exercises one of these (online without a URL) end to end; the rest are
 * quicker to nail down here.
 */
class ValidEventRequestValidatorTest {

    private final ValidEventRequestValidator validator = new ValidEventRequestValidator();

    @Test
    void aNullRequestOrAMissingFormatIsLeftToOtherConstraints() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid(request(null, null, null, null, null, null), null)).isTrue();
    }

    @Test
    void inPersonAndHybridEventsNeedAVenue() {
        assertThat(validator.isValid(request(EventFormat.IN_PERSON, null, null, null, null, "UTC"), null)).isFalse();
        assertThat(validator.isValid(request(EventFormat.HYBRID, null, null, null, null, "UTC"), null)).isFalse();
        assertThat(validator.isValid(request(EventFormat.IN_PERSON, UUID.randomUUID(), null, null, null, "UTC"), null))
                .isTrue();
    }

    @Test
    void onlineAndHybridEventsNeedAUrl() {
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, null, null, null, "UTC"), null)).isFalse();
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, "", null, null, "UTC"), null)).isFalse();
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, "https://example.test", null, null, "UTC"), null))
                .isTrue();
    }

    @Test
    void endsAtMustBeAfterStartsAtWhenBothArePresent() {
        Instant starts = Instant.parse("2027-01-01T18:00:00Z");
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, "https://x.test",
                starts, starts.minusSeconds(1), "UTC"), null)).isFalse();
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, "https://x.test",
                starts, starts, "UTC"), null)).isFalse();
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, "https://x.test",
                starts, starts.plusSeconds(1), "UTC"), null)).isTrue();
        // No endsAt at all is fine — it is optional.
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, "https://x.test",
                starts, null, "UTC"), null)).isTrue();
    }

    @Test
    void theTimeZoneMustBeARealIanaId() {
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, "https://x.test", null, null,
                "Not/AZone"), null)).isFalse();
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, "https://x.test", null, null,
                "Europe/Bucharest"), null)).isTrue();
        // No time zone at all is left to @NotBlank on the field itself.
        assertThat(validator.isValid(request(EventFormat.ONLINE, null, "https://x.test", null, null, null), null))
                .isTrue();
    }

    private static EventLocationAndSchedule request(EventFormat format, UUID venueId, String onlineUrl,
                                                     Instant startsAt, Instant endsAt, String timeZone) {
        return new EventLocationAndSchedule() {
            public EventFormat format() {
                return format;
            }

            public UUID venueId() {
                return venueId;
            }

            public String onlineUrl() {
                return onlineUrl;
            }

            public Instant startsAt() {
                return startsAt;
            }

            public Instant endsAt() {
                return endsAt;
            }

            public String timeZone() {
                return timeZone;
            }
        };
    }
}
