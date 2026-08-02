package com.showup.api.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventCommentRepository extends JpaRepository<EventComment, UUID> {

    List<EventComment> findAllByEventIdOrderByCreatedAtAsc(UUID eventId);
}
