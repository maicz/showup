package com.showup.api.attendance;

import com.showup.api.event.Event;
import com.showup.api.member.Member;
import com.showup.api.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** Rolls up into {@code Group.ratingAverage}. See docs/domain-model.md#eventcomment-eventphoto-eventfeedback. */
@Entity
@Table(name = "event_feedback", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_feedback_event_member", columnNames = {"event_id", "member_id"})
})
public class EventFeedback extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_feedback_event"))
    private Event event;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_feedback_member"))
    private Member member;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected EventFeedback() {
        // for JPA
    }

    public EventFeedback(Event event, Member member, int rating, String comment) {
        this.event = event;
        this.member = member;
        this.rating = rating;
        this.comment = comment;
        this.submittedAt = Instant.now();
    }

    public Event getEvent() {
        return event;
    }

    public Member getMember() {
        return member;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
