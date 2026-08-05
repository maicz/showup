package com.showup.api.repository;

import com.showup.api.entity.EventFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventFeedbackRepository extends JpaRepository<EventFeedback, UUID> {

    List<EventFeedback> findAllByEventId(UUID eventId);

    Optional<EventFeedback> findByEventIdAndMemberId(UUID eventId, UUID memberId);

    @Query("select avg(f.rating) from EventFeedback f where f.event.group.id = :groupId")
    Double averageRatingForGroup(UUID groupId);

    @Query("select count(f) from EventFeedback f where f.event.group.id = :groupId")
    long countForGroup(UUID groupId);

    @Query("select avg(f.rating) from EventFeedback f "
            + "where f.event.group.id = :groupId and f.submittedAt between :from and :to")
    Double averageRatingForGroupBetween(UUID groupId, Instant from, Instant to);
}
