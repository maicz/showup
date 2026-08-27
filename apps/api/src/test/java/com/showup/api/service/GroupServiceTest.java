package com.showup.api.service;

import com.showup.api.entity.Group;
import com.showup.api.entity.GroupMembership;
import com.showup.api.entity.Member;
import com.showup.api.enums.GroupMemberRole;
import com.showup.api.enums.GroupMembershipStatus;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.GroupMapper;
import com.showup.api.mapper.MemberMapper;
import com.showup.api.mapper.TopicMapper;
import com.showup.api.repository.CategoryRepository;
import com.showup.api.repository.GroupMembershipRepository;
import com.showup.api.repository.GroupRepository;
import com.showup.api.repository.GroupTopicRepository;
import com.showup.api.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groups;
    @Mock
    private GroupMembershipRepository memberships;
    @Mock
    private GroupTopicRepository groupTopics;
    @Mock
    private CategoryRepository categories;
    @Mock
    private TopicRepository topics;
    @Mock
    private MemberService memberService;
    @Mock
    private GroupAccessGuard guard;
    @Mock
    private GroupMapper groupMapper;
    @Mock
    private TopicMapper topicMapper;
    @Mock
    private MemberMapper memberMapper;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(
                groups, memberships, groupTopics, categories, topics,
                memberService, guard, groupMapper, topicMapper, memberMapper
        );
    }

    @Test
    void declineMemberTransitionsPendingStatusToLeft() {
        UUID actorId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        GroupMembership membership = new GroupMembership(mock(Group.class), mock(Member.class), GroupMemberRole.MEMBER, null);
        membership.setStatus(GroupMembershipStatus.PENDING_APPROVAL);

        when(memberships.findByGroupIdAndMemberId(groupId, memberId)).thenReturn(Optional.of(membership));

        groupService.declineMember(actorId, groupId, memberId);

        verify(guard).requireGroupAdmin(groupId, actorId);
        assertThat(membership.getStatus()).isEqualTo(GroupMembershipStatus.LEFT);
    }

    @Test
    void declineMemberThrowsIfMembershipIsNotPendingApproval() {
        UUID actorId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        GroupMembership membership = new GroupMembership(mock(Group.class), mock(Member.class), GroupMemberRole.MEMBER, null);
        membership.setStatus(GroupMembershipStatus.ACTIVE);

        when(memberships.findByGroupIdAndMemberId(groupId, memberId)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> groupService.declineMember(actorId, groupId, memberId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not PENDING_APPROVAL");
    }

    @Test
    void declineMemberThrowsIfMembershipNotFound() {
        UUID actorId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(memberships.findByGroupIdAndMemberId(groupId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.declineMember(actorId, groupId, memberId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("is not in this group");
    }
}
