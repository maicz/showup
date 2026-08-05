package com.showup.api.entity;

import com.showup.api.enums.CheckInMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/** The unique FK on {@link #ticket} is what makes double-scanning idempotent instead of double-counting. */
@Entity
@Table(name = "check_in")
public class CheckIn extends BaseEntity {

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_check_in_ticket"))
    private Ticket ticket;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checked_in_by_id", nullable = false, foreignKey = @ForeignKey(name = "fk_check_in_checked_in_by"))
    private Member checkedInBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckInMethod method;

    @Positive
    @Column(name = "admitted_count", nullable = false)
    private int admittedCount;

    protected CheckIn() {
        // for JPA
    }

    public CheckIn(Ticket ticket, Member checkedInBy, CheckInMethod method, int admittedCount) {
        this.ticket = ticket;
        this.checkedInAt = Instant.now();
        this.checkedInBy = checkedInBy;
        this.method = method;
        this.admittedCount = admittedCount;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public Member getCheckedInBy() {
        return checkedInBy;
    }

    public CheckInMethod getMethod() {
        return method;
    }

    public int getAdmittedCount() {
        return admittedCount;
    }
}
