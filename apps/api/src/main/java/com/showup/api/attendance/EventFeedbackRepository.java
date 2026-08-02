package com.showup.api.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventFeedbackRepository extends JpaRepository<EventFeedback, UUID> {

    List<EventFeedback> findAllByEventId(UUID eventId);

    Optional<EventFeedback> findByEventIdAndMemberId(UUID eventId, UUID memberId);
}
