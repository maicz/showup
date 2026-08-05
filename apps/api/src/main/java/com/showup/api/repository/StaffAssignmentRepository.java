package com.showup.api.repository;

import com.showup.api.entity.StaffAssignment;
import com.showup.api.enums.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, UUID> {

    List<StaffAssignment> findAllByEventId(UUID eventId);

    List<StaffAssignment> findAllByEventIdAndRole(UUID eventId, StaffRole role);

    List<StaffAssignment> findAllByMemberId(UUID memberId);

    /** Backs the check-in authorization rule: an assigned SCANNER may scan without being staff-level in the group. */
    boolean existsByEventIdAndMemberIdAndRole(UUID eventId, UUID memberId, StaffRole role);
}
