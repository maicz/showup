package com.showup.api.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {

    Optional<GroupMembership> findByGroupIdAndMemberId(UUID groupId, UUID memberId);

    List<GroupMembership> findAllByGroupIdAndStatus(UUID groupId, GroupMembershipStatus status);

    List<GroupMembership> findAllByMemberIdAndStatus(UUID memberId, GroupMembershipStatus status);
}
