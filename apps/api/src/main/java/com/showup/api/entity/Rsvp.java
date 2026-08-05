package com.showup.api.entity;

import com.showup.api.enums.RsvpStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * The unique constraint on {@code (event_id, member_id)} is the database-level guarantee that a
 * member cannot double-book by double-clicking. Seat allocation is a transaction that takes a
 * row lock on {@code event}, re-reads {@code yesRsvpCount}, and either seats or waitlists — see
 * docs/domain-model.md#rsvp.
 */
@Entity
@Table(name = "rsvp", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rsvp_event_member", columnNames = {"event_id", "member_id"})
})
public class Rsvp extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rsvp_event"))
    private Event event;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rsvp_member"))
    private Member member;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RsvpStatus status;

    @PositiveOrZero
    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    @Column(name = "responded_at", nullable = false)
    private Instant respondedAt;

    @Column(name = "promoted_at")
    private Instant promotedAt;

    protected Rsvp() {
        // for JPA
    }

    public Rsvp(Event event, Member member, RsvpStatus status, int guestCount) {
        this.event = event;
        this.member = member;
        this.status = status;
        this.guestCount = guestCount;
        this.respondedAt = Instant.now();
    }

    public Event getEvent() {
        return event;
    }

    public Member getMember() {
        return member;
    }

    public RsvpStatus getStatus() {
        return status;
    }

    public void setStatus(RsvpStatus status) {
        this.status = status;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public Integer getWaitlistPosition() {
        return waitlistPosition;
    }

    public void setWaitlistPosition(Integer waitlistPosition) {
        this.waitlistPosition = waitlistPosition;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Instant getPromotedAt() {
        return promotedAt;
    }

    public void setPromotedAt(Instant promotedAt) {
        this.promotedAt = promotedAt;
    }
}
