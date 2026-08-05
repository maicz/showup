package com.showup.api.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceRulesTest {

    private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");

    @Test
    void weeklyRuleWithCountProducesThatManyOccurrences() {
        List<Instant> occurrences = RecurrenceRules.expand(
                RecurrenceRules.parse("FREQ=WEEKLY;INTERVAL=1;COUNT=3"),
                Instant.parse("2026-09-02T16:00:00Z"), BUCHAREST, null, 52);

        assertThat(occurrences).containsExactly(
                Instant.parse("2026-09-02T16:00:00Z"),
                Instant.parse("2026-09-09T16:00:00Z"),
                Instant.parse("2026-09-16T16:00:00Z"));
    }

    /**
     * The reason the arithmetic runs in the series' zone: Bucharest leaves DST on 25 October
     * 2026, so a 19:00 local meetup is 16:00Z before the change and 17:00Z after it. Adding a
     * fixed 168 hours would silently move the meetup to 18:00 local.
     */
    @Test
    void weeklyRuleKeepsLocalWallClockTimeAcrossADaylightSavingChange() {
        List<Instant> occurrences = RecurrenceRules.expand(
                RecurrenceRules.parse("FREQ=WEEKLY;COUNT=2"),
                Instant.parse("2026-10-22T16:00:00Z"), BUCHAREST, null, 52);

        assertThat(occurrences).containsExactly(
                Instant.parse("2026-10-22T16:00:00Z"),
                Instant.parse("2026-10-29T17:00:00Z"));
        assertThat(occurrences.stream().map(i -> i.atZone(BUCHAREST).toLocalTime().toString()))
                .containsOnly("19:00");
    }

    @Test
    void untilStopsTheSeriesEvenWhenCountWouldNot() {
        List<Instant> occurrences = RecurrenceRules.expand(
                RecurrenceRules.parse("FREQ=DAILY;COUNT=10;UNTIL=20260904T235959Z"),
                Instant.parse("2026-09-02T16:00:00Z"), BUCHAREST, null, 52);

        assertThat(occurrences).hasSize(3);
    }

    @Test
    void theSeriesLevelUntilAlsoCaps() {
        List<Instant> occurrences = RecurrenceRules.expand(
                RecurrenceRules.parse("FREQ=DAILY;COUNT=10"),
                Instant.parse("2026-09-02T16:00:00Z"), BUCHAREST,
                Instant.parse("2026-09-03T23:59:59Z"), 52);

        assertThat(occurrences).hasSize(2);
    }

    @Test
    void theHardLimitBeatsAnUnboundedRule() {
        List<Instant> occurrences = RecurrenceRules.expand(
                RecurrenceRules.parse("FREQ=DAILY"),
                Instant.parse("2026-09-02T16:00:00Z"), BUCHAREST, null, 5);

        assertThat(occurrences).hasSize(5);
    }

    @Test
    void unsupportedPartsAreRejectedRatherThanIgnored() {
        assertThatThrownBy(() -> RecurrenceRules.parse("FREQ=WEEKLY;BYDAY=TU,TH"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BYDAY");
        assertThatThrownBy(() -> RecurrenceRules.parse("INTERVAL=2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FREQ");
        assertThatThrownBy(() -> RecurrenceRules.parse("FREQ=FORTNIGHTLY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported FREQ");
    }

    @Test
    void aBlankOrMissingRuleIsRejected() {
        assertThatThrownBy(() -> RecurrenceRules.parse(null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("empty");
        assertThatThrownBy(() -> RecurrenceRules.parse("   "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("empty");
    }

    @Test
    void aPartWithNoEqualsSignIsMalformed() {
        assertThatThrownBy(() -> RecurrenceRules.parse("FREQ=WEEKLY;GARBAGE"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("malformed");
    }

    @Test
    void intervalAndCountMustBePositiveNumbers() {
        assertThatThrownBy(() -> RecurrenceRules.parse("FREQ=WEEKLY;INTERVAL=zero"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("INTERVAL must be a number");
        assertThatThrownBy(() -> RecurrenceRules.parse("FREQ=WEEKLY;INTERVAL=0"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("INTERVAL must be positive");
        assertThatThrownBy(() -> RecurrenceRules.parse("FREQ=WEEKLY;COUNT=abc"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("COUNT must be a number");
    }

    @Test
    void untilMustMatchTheExpectedFormat() {
        assertThatThrownBy(() -> RecurrenceRules.parse("FREQ=WEEKLY;UNTIL=not-a-date"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("UNTIL must look like");
    }

    @Test
    void monthlyAndYearlyFrequenciesStepByCalendarUnitsNotFixedDurations() {
        List<Instant> monthly = RecurrenceRules.expand(RecurrenceRules.parse("FREQ=MONTHLY;COUNT=2"),
                Instant.parse("2026-01-31T10:00:00Z"), BUCHAREST, null, 52);
        assertThat(monthly).hasSize(2);
        assertThat(monthly.get(1).atZone(BUCHAREST).getMonthValue()).isEqualTo(2);

        List<Instant> yearly = RecurrenceRules.expand(RecurrenceRules.parse("FREQ=YEARLY;COUNT=2"),
                Instant.parse("2026-03-01T10:00:00Z"), BUCHAREST, null, 52);
        assertThat(yearly).hasSize(2);
        assertThat(yearly.get(1).atZone(BUCHAREST).getYear()).isEqualTo(2027);
    }

    @Test
    void whicheverOfTheRuleOrSeriesUntilComesFirstWinsInEitherDirection() {
        List<Instant> ruleUntilWins = RecurrenceRules.expand(
                RecurrenceRules.parse("FREQ=DAILY;COUNT=10;UNTIL=20260903T235959Z"),
                Instant.parse("2026-09-02T16:00:00Z"), BUCHAREST,
                Instant.parse("2026-09-10T23:59:59Z"), 52);
        assertThat(ruleUntilWins).hasSize(2);

        List<Instant> seriesUntilWins = RecurrenceRules.expand(
                RecurrenceRules.parse("FREQ=DAILY;COUNT=10;UNTIL=20260910T235959Z"),
                Instant.parse("2026-09-02T16:00:00Z"), BUCHAREST,
                Instant.parse("2026-09-03T23:59:59Z"), 52);
        assertThat(seriesUntilWins).hasSize(2);
    }
}
