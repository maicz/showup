package com.showup.api.entity;

import com.showup.api.enums.IdentityProvider;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One row per linked SSO provider — a member can link several. See
 * docs/domain-model.md#memberidentity. Unique on {@code (provider, subject)} and on
 * {@code (member_id, provider)}.
 */
@Entity
@Table(name = "member_identity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_identity_provider_subject", columnNames = {"provider", "subject"}),
        @UniqueConstraint(name = "uk_member_identity_member_provider", columnNames = {"member_id", "provider"})
})
public class MemberIdentity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_identity_member"))
    private Member member;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdentityProvider provider;

    @NotBlank
    @Column(nullable = false)
    private String subject;

    protected MemberIdentity() {
        // for JPA
    }

    public MemberIdentity(Member member, IdentityProvider provider, String subject) {
        this.member = member;
        this.provider = provider;
        this.subject = subject;
    }

    public Member getMember() {
        return member;
    }

    public IdentityProvider getProvider() {
        return provider;
    }

    public String getSubject() {
        return subject;
    }
}
