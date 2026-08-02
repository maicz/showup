package com.showup.api.event;

import com.showup.api.group.Group;
import com.showup.api.shared.BaseEntity;
import com.showup.api.shared.Money;
import com.showup.api.venue.Venue;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * The heart of the model. See docs/domain-model.md#event for the reasoning behind storing both
 * the instant and the IANA zone, money as integer minor units, and a nullable rather than
 * sentinel {@link #capacity}.
 */
@Entity
@Table(name = "event")
public class Event extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_group"))
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", foreignKey = @ForeignKey(name = "fk_event_series"))
    private EventSeries series;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventFormat format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", foreignKey = @ForeignKey(name = "fk_event_venue"))
    private Venue venue;

    @Column(name = "online_url", length = 500)
    private String onlineUrl;

    @NotNull
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @NotBlank
    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "waitlist_enabled", nullable = false)
    private boolean waitlistEnabled;

    @Column(name = "guests_per_rsvp_limit", nullable = false)
    private int guestsPerRsvpLimit;

    @Embedded
    @AttributeOverride(name = "amountMinor", column = @Column(name = "fee_amount_minor", nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "fee_currency", length = 3, nullable = false))
    private Money fee;

    @Column(name = "rsvp_opens_at")
    private Instant rsvpOpensAt;

    @Column(name = "rsvp_closes_at")
    private Instant rsvpClosesAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventVisibility visibility;

    @Column(name = "yes_rsvp_count", nullable = false)
    private int yesRsvpCount;

    @Column(name = "waitlist_count", nullable = false)
    private int waitlistCount;

    protected Event() {
        // for JPA
    }

    public Event(Group group, String title, EventFormat format, Instant startsAt, String timeZone, Money fee) {
        this.group = group;
        this.title = title;
        this.format = format;
        this.startsAt = startsAt;
        this.timeZone = timeZone;
        this.fee = fee;
        this.status = EventStatus.DRAFT;
        this.visibility = EventVisibility.PUBLIC;
        this.waitlistEnabled = false;
        this.guestsPerRsvpLimit = 0;
        this.yesRsvpCount = 0;
        this.waitlistCount = 0;
    }

    public Group getGroup() {
        return group;
    }

    public EventSeries getSeries() {
        return series;
    }

    public void setSeries(EventSeries series) {
        this.series = series;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public EventFormat getFormat() {
        return format;
    }

    public void setFormat(EventFormat format) {
        this.format = format;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public String getOnlineUrl() {
        return onlineUrl;
    }

    public void setOnlineUrl(String onlineUrl) {
        this.onlineUrl = onlineUrl;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public boolean isWaitlistEnabled() {
        return waitlistEnabled;
    }

    public void setWaitlistEnabled(boolean waitlistEnabled) {
        this.waitlistEnabled = waitlistEnabled;
    }

    public int getGuestsPerRsvpLimit() {
        return guestsPerRsvpLimit;
    }

    public void setGuestsPerRsvpLimit(int guestsPerRsvpLimit) {
        this.guestsPerRsvpLimit = guestsPerRsvpLimit;
    }

    public Money getFee() {
        return fee;
    }

    public void setFee(Money fee) {
        this.fee = fee;
    }

    public Instant getRsvpOpensAt() {
        return rsvpOpensAt;
    }

    public void setRsvpOpensAt(Instant rsvpOpensAt) {
        this.rsvpOpensAt = rsvpOpensAt;
    }

    public Instant getRsvpClosesAt() {
        return rsvpClosesAt;
    }

    public void setRsvpClosesAt(Instant rsvpClosesAt) {
        this.rsvpClosesAt = rsvpClosesAt;
    }

    public EventVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(EventVisibility visibility) {
        this.visibility = visibility;
    }

    public int getYesRsvpCount() {
        return yesRsvpCount;
    }

    public void setYesRsvpCount(int yesRsvpCount) {
        this.yesRsvpCount = yesRsvpCount;
    }

    public int getWaitlistCount() {
        return waitlistCount;
    }

    public void setWaitlistCount(int waitlistCount) {
        this.waitlistCount = waitlistCount;
    }

    /** Computes the Full / Waitlist / Seats-available badge shown on every card and detail page. */
    public AvailabilityState getAvailability() {
        if (capacity == null) {
            return AvailabilityState.SEATS_AVAILABLE;
        }
        if (yesRsvpCount < capacity) {
            return AvailabilityState.SEATS_AVAILABLE;
        }
        return waitlistEnabled ? AvailabilityState.WAITLIST : AvailabilityState.FULL;
    }
}
