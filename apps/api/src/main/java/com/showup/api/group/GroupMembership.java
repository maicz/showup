package com.showup.api.group;

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

/**
 * The authorization table: who may edit an event, scan tickets, or see the attendee list is a
 * function of {@link #role} here. See docs/domain-model.md#groupmembership.
 */
@Entity
@Table(name = "group_membership", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_membership_group_member", columnNames = {"group_id", "member_id"})
})
public class GroupMembership extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(name = "fk_group_membership_group"))
    private Group group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_group_membership_member"))
    private Member member;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GroupMemberRole role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMembershipStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(columnDefinition = "text")
    private String introduction;

    protected GroupMembership() {
        // for JPA
    }

    public GroupMembership(Group group, Member member, GroupMemberRole role, String introduction) {
        this.group = group;
        this.member = member;
        this.role = role;
        this.status = GroupMembershipStatus.ACTIVE;
        this.joinedAt = Instant.now();
        this.introduction = introduction;
    }

    public Group getGroup() {
        return group;
    }

    public Member getMember() {
        return member;
    }

    public GroupMemberRole getRole() {
        return role;
    }

    public void setRole(GroupMemberRole role) {
        this.role = role;
    }

    public GroupMembershipStatus getStatus() {
        return status;
    }

    public void setStatus(GroupMembershipStatus status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public String getIntroduction() {
        return introduction;
    }
}
