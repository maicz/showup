package com.showup.api.shared;

import jakarta.persistence.Embeddable;

/**
 * Integer minor units plus an ISO 4217 currency code — never a floating point amount. See
 * docs/domain-model.md#event, "Money as integer minor units plus a currency code".
 *
 * <p>A record so it doubles as the wire-format DTO used directly in {@code EventSummary} /
 * {@code EventDetail} — Hibernate maps records as embeddables via their canonical constructor,
 * and column names ({@code amount_minor}, {@code currency}) fall out of the default naming
 * strategy, so no {@code @Column} overrides are needed.
 */
@Embeddable
public record Money(long amountMinor, String currency) {

    public static Money zero(String currency) {
        return new Money(0, currency);
    }
}
