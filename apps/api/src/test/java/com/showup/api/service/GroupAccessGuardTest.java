package com.showup.api.service;

import com.showup.api.entity.GroupMembership;
import com.showup.api.enums.GroupMemberRole;
import com.showup.api.enums.GroupMembershipStatus;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.repository.GroupMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Plain Mockito unit tests, no Spring context: {@link GroupAccessGuard} is a pure function of a
 * repository lookup plus a role set, so mocking the one dependency exercises every branch far
 * faster than driving it through HTTP.
 */
@ExtendWith(MockitoExtension.class)
class GroupAccessGuardTest {

    @Mock
    private GroupMembershipRepository memberships;

    private GroupAccessGuard guard;

    private final UUID groupId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        guard = new GroupAccessGuard(memberships);
    }

    @Test
    void aMissingMembershipFailsActiveMembershipChecks() {
        when(memberships.findByGroupIdAndMemberId(groupId, memberId)).thenReturn(Optional.empty());

        assertThat(guard.activeMembership(groupId, memberId)).isEmpty();
        assertThat(guard.isEventAdmin(groupId, memberId)).isFalse();
        assertThatThrownBy(() -> guard.requireActiveMember(groupId, memberId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aBannedOrPendingMembershipDoesNotCountAsActive() {
        GroupMembership pending = org.mockito.Mockito.mock(GroupMembership.class);
        when(pending.getStatus()).thenReturn(GroupMembershipStatus.PENDING_APPROVAL);
        when(memberships.findByGroupIdAndMemberId(groupId, memberId)).thenReturn(Optional.of(pending));

        assertThat(guard.activeMembership(groupId, memberId)).isEmpty();
        assertThatThrownBy(() -> guard.requireActiveMember(groupId, memberId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aPlainMemberIsNeitherAGroupAdminNorAnEventAdmin() {
        GroupMembership member = membership(GroupMemberRole.MEMBER, GroupMembershipStatus.ACTIVE);
        when(memberships.findByGroupIdAndMemberId(groupId, memberId)).thenReturn(Optional.of(member));

        assertThat(guard.isEventAdmin(groupId, memberId)).isFalse();
        assertThatThrownBy(() -> guard.requireGroupAdmin(groupId, memberId))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> guard.requireEventAdmin(groupId, memberId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void anEventOrganizerRoleIsAnEventAdminButNotNecessarilyAGroupAdmin() {
        GroupMembership eventOrganizer = membership(GroupMemberRole.EVENT_ORGANIZER, GroupMembershipStatus.ACTIVE);
        when(memberships.findByGroupIdAndMemberId(groupId, memberId)).thenReturn(Optional.of(eventOrganizer));

        assertThat(guard.isEventAdmin(groupId, memberId)).isTrue();
        assertThat(guard.requireEventAdmin(groupId, memberId)).isSameAs(eventOrganizer);
        assertThatThrownBy(() -> guard.requireGroupAdmin(groupId, memberId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void anOrganizerIsBothAGroupAdminAndAnEventAdmin() {
        GroupMembership organizer = membership(GroupMemberRole.ORGANIZER, GroupMembershipStatus.ACTIVE);
        when(memberships.findByGroupIdAndMemberId(groupId, memberId)).thenReturn(Optional.of(organizer));

        assertThat(guard.requireGroupAdmin(groupId, memberId)).isSameAs(organizer);
        assertThat(guard.requireEventAdmin(groupId, memberId)).isSameAs(organizer);
    }

    private GroupMembership membership(GroupMemberRole role, GroupMembershipStatus status) {
        GroupMembership membership = org.mockito.Mockito.mock(GroupMembership.class);
        when(membership.getRole()).thenReturn(role);
        when(membership.getStatus()).thenReturn(status);
        return membership;
    }
}
