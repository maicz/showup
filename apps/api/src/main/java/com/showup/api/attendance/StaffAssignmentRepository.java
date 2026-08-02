package com.showup.api.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, UUID> {

    List<StaffAssignment> findAllByEventId(UUID eventId);

    List<StaffAssignment> findAllByEventIdAndRole(UUID eventId, StaffRole role);

    List<StaffAssignment> findAllByMemberId(UUID memberId);
}
