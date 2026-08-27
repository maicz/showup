package com.showup.api.repository;

import com.showup.api.entity.GroupMembership;
import com.showup.api.enums.GroupMemberRole;
import com.showup.api.enums.GroupMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {

    Optional<GroupMembership> findByGroupIdAndMemberId(UUID groupId, UUID memberId);

    List<GroupMembership> findAllByGroupIdAndStatus(UUID groupId, GroupMembershipStatus status);

    /** The group's public "founder & organizer" byline — oldest active ORGANIZER row wins. */
    Optional<GroupMembership> findFirstByGroupIdAndRoleAndStatusOrderByCreatedAtAsc(
            UUID groupId, GroupMemberRole role, GroupMembershipStatus status);

    List<GroupMembership> findAllByMemberIdAndStatus(UUID memberId, GroupMembershipStatus status);

    /** Guards the "a group must keep at least one organizer" rule on demotion and departure. */
    long countByGroupIdAndRoleAndStatus(UUID groupId, GroupMemberRole role, GroupMembershipStatus status);
}
