package com.showup.api.service;

import com.showup.api.entity.GroupMembership;
import com.showup.api.enums.GroupMemberRole;
import com.showup.api.enums.GroupMembershipStatus;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.repository.GroupMembershipRepository;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The single place that answers "may this member do this to this group?".
 *
 * <p>Authorization here is a function of {@link GroupMemberRole} in the {@code group_membership}
 * row, not of a token scope — see docs/domain-model.md#groupmembership. That is why it cannot
 * live in the security filter chain: the answer depends on which group the request touches.
 */
@Component
public class GroupAccessGuard {

    /** Group settings, membership roles, deleting the group. */
    private static final Set<GroupMemberRole> GROUP_ADMINS =
            EnumSet.of(GroupMemberRole.ORGANIZER, GroupMemberRole.CO_ORGANIZER);

    /** Creating and running events. Broader, because that is the whole point of the delegated roles. */
    private static final Set<GroupMemberRole> EVENT_ADMINS = EnumSet.of(
            GroupMemberRole.ORGANIZER,
            GroupMemberRole.CO_ORGANIZER,
            GroupMemberRole.ASSISTANT_ORGANIZER,
            GroupMemberRole.EVENT_ORGANIZER);

    private final GroupMembershipRepository memberships;

    GroupAccessGuard(GroupMembershipRepository memberships) {
        this.memberships = memberships;
    }

    public Optional<GroupMembership> activeMembership(UUID groupId, UUID memberId) {
        return memberships.findByGroupIdAndMemberId(groupId, memberId)
                .filter(m -> m.getStatus() == GroupMembershipStatus.ACTIVE);
    }

    public GroupMembership requireActiveMember(UUID groupId, UUID memberId) {
        return activeMembership(groupId, memberId)
                .orElseThrow(() -> new ForbiddenException("not an active member of this group"));
    }

    public GroupMembership requireGroupAdmin(UUID groupId, UUID memberId) {
        GroupMembership membership = requireActiveMember(groupId, memberId);
        if (!GROUP_ADMINS.contains(membership.getRole())) {
            throw new ForbiddenException("requires the ORGANIZER or CO_ORGANIZER role");
        }
        return membership;
    }

    public GroupMembership requireEventAdmin(UUID groupId, UUID memberId) {
        GroupMembership membership = requireActiveMember(groupId, memberId);
        if (!EVENT_ADMINS.contains(membership.getRole())) {
            throw new ForbiddenException("requires an organizer role in this group");
        }
        return membership;
    }

    public boolean isEventAdmin(UUID groupId, UUID memberId) {
        return activeMembership(groupId, memberId)
                .map(m -> EVENT_ADMINS.contains(m.getRole()))
                .orElse(false);
    }
}
