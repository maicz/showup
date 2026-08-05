package com.showup.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * One per seated RSVP, carrying the QR payload. Separate from {@link Rsvp} because a ticket has
 * its own lifecycle: it can be reissued without disturbing the RSVP. See
 * docs/domain-model.md#ticket.
 */
@Entity
@Table(name = "ticket")
public class Ticket extends BaseEntity {

    private static final SecureRandom RANDOM = new SecureRandom();

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rsvp_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_ticket_rsvp"))
    private Rsvp rsvp;

    @NotBlank
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Positive
    @Column(name = "admit_count", nullable = false)
    private int admitCount;

    protected Ticket() {
        // for JPA
    }

    public Ticket(Rsvp rsvp, int admitCount) {
        this.rsvp = rsvp;
        this.code = generateCode();
        this.issuedAt = Instant.now();
        this.admitCount = admitCount;
    }

    /** A random 128-bit value, not the RSVP id — a guessable code is a free entry. */
    private static String generateCode() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Rsvp getRsvp() {
        return rsvp;
    }

    public String getCode() {
        return code;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public int getAdmitCount() {
        return admitCount;
    }

    public void setAdmitCount(int admitCount) {
        this.admitCount = admitCount;
    }
}
