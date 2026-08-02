package com.showup.api.event;

import com.showup.api.member.Member;
import com.showup.api.shared.BaseEntity;
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

/**
 * Distinct from {@code StaffAssignment} — hosts are public-facing and shown on the event page;
 * staff are operational. See docs/domain-model.md#eventhost.
 */
@Entity
@Table(name = "event_host", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_host_event_member", columnNames = {"event_id", "member_id"})
})
public class EventHost extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_host_event"))
    private Event event;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_host_member"))
    private Member member;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventHostRole role;

    protected EventHost() {
        // for JPA
    }

    public EventHost(Event event, Member member, EventHostRole role) {
        this.event = event;
        this.member = member;
        this.role = role;
    }

    public Event getEvent() {
        return event;
    }

    public Member getMember() {
        return member;
    }

    public EventHostRole getRole() {
        return role;
    }
}
