package com.showup.api.event;

import com.showup.api.group.Group;
import com.showup.api.shared.BaseEntity;
import com.showup.api.venue.Venue;
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

/**
 * Recurrence, kept separate so occurrences are real {@link Event} rows that can each be
 * individually cancelled, re-venued, or over-attended. Occurrences are materialized ahead of
 * time rather than computed on read from {@link #recurrenceRule} — an RRULE cannot be indexed,
 * joined against RSVPs, or reported on. See docs/domain-model.md#eventseries.
 */
@Entity
@Table(name = "event_series")
public class EventSeries extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_series_group"))
    private Group group;

    @NotBlank
    @Column(name = "recurrence_rule", nullable = false, columnDefinition = "text")
    private String recurrenceRule;

    @Column(name = "until")
    private Instant until;

    @NotBlank
    @Column(name = "template_title", nullable = false)
    private String templateTitle;

    @Column(name = "template_description", columnDefinition = "text")
    private String templateDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_venue_id", foreignKey = @ForeignKey(name = "fk_event_series_venue"))
    private Venue templateVenue;

    @Column(name = "template_duration_minutes", nullable = false)
    private int templateDurationMinutes;

    protected EventSeries() {
        // for JPA
    }

    public EventSeries(Group group, String recurrenceRule, String templateTitle, int templateDurationMinutes) {
        this.group = group;
        this.recurrenceRule = recurrenceRule;
        this.templateTitle = templateTitle;
        this.templateDurationMinutes = templateDurationMinutes;
    }

    public Group getGroup() {
        return group;
    }

    public String getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(String recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
    }

    public Instant getUntil() {
        return until;
    }

    public void setUntil(Instant until) {
        this.until = until;
    }

    public String getTemplateTitle() {
        return templateTitle;
    }

    public void setTemplateTitle(String templateTitle) {
        this.templateTitle = templateTitle;
    }

    public String getTemplateDescription() {
        return templateDescription;
    }

    public void setTemplateDescription(String templateDescription) {
        this.templateDescription = templateDescription;
    }

    public Venue getTemplateVenue() {
        return templateVenue;
    }

    public void setTemplateVenue(Venue templateVenue) {
        this.templateVenue = templateVenue;
    }

    public int getTemplateDurationMinutes() {
        return templateDurationMinutes;
    }

    public void setTemplateDurationMinutes(int templateDurationMinutes) {
        this.templateDurationMinutes = templateDurationMinutes;
    }
}
