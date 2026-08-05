package com.showup.api.repository;

import com.showup.api.entity.EventComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventCommentRepository extends JpaRepository<EventComment, UUID> {

    List<EventComment> findAllByEventIdOrderByCreatedAtAsc(UUID eventId);
}
