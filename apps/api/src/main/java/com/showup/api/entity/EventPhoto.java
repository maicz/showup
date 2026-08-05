package com.showup.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Stores the URL only — bytes belong in object storage, never in Postgres. */
@Entity
@Table(name = "event_photo")
public class EventPhoto extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_photo_event"))
    private Event event;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_photo_uploaded_by"))
    private Member uploadedBy;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String url;

    private String caption;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    protected EventPhoto() {
        // for JPA
    }

    public EventPhoto(Event event, Member uploadedBy, String url, String caption, int width, int height) {
        this.event = event;
        this.uploadedBy = uploadedBy;
        this.url = url;
        this.caption = caption;
        this.width = width;
        this.height = height;
    }

    public Event getEvent() {
        return event;
    }

    public Member getUploadedBy() {
        return uploadedBy;
    }

    public String getUrl() {
        return url;
    }

    public String getCaption() {
        return caption;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
