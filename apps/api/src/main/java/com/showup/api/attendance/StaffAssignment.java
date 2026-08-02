package com.showup.api.attendance;

import com.showup.api.event.Event;
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

import java.time.Instant;

@Entity
@Table(name = "staff_assignment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_assignment_event_member_role", columnNames = {"event_id", "member_id", "role"})
})
public class StaffAssignment extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staff_assignment_event"))
    private Event event;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staff_assignment_member"))
    private Member member;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole role;

    @Column(name = "shift_starts_at")
    private Instant shiftStartsAt;

    @Column(name = "shift_ends_at")
    private Instant shiftEndsAt;

    @Column(columnDefinition = "text")
    private String notes;

    protected StaffAssignment() {
        // for JPA
    }

    public StaffAssignment(Event event, Member member, StaffRole role, Instant shiftStartsAt, Instant shiftEndsAt) {
        this.event = event;
        this.member = member;
        this.role = role;
        this.shiftStartsAt = shiftStartsAt;
        this.shiftEndsAt = shiftEndsAt;
    }

    public Event getEvent() {
        return event;
    }

    public Member getMember() {
        return member;
    }

    public StaffRole getRole() {
        return role;
    }

    public Instant getShiftStartsAt() {
        return shiftStartsAt;
    }

    public void setShiftStartsAt(Instant shiftStartsAt) {
        this.shiftStartsAt = shiftStartsAt;
    }

    public Instant getShiftEndsAt() {
        return shiftEndsAt;
    }

    public void setShiftEndsAt(Instant shiftEndsAt) {
        this.shiftEndsAt = shiftEndsAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
