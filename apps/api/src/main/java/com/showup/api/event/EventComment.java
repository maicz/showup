package com.showup.api.event;

import com.showup.api.member.Member;
import com.showup.api.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** One level of replies via {@link #parentComment}; soft-deleted so reply threads survive a removed parent. */
@Entity
@Table(name = "event_comment")
public class EventComment extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_comment_event"))
    private Event event;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_comment_author"))
    private Member author;

    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", foreignKey = @ForeignKey(name = "fk_event_comment_parent"))
    private EventComment parentComment;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected EventComment() {
        // for JPA
    }

    public EventComment(Event event, Member author, String body, EventComment parentComment) {
        this.event = event;
        this.author = author;
        this.body = body;
        this.parentComment = parentComment;
    }

    public Event getEvent() {
        return event;
    }

    public Member getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public EventComment getParentComment() {
        return parentComment;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
