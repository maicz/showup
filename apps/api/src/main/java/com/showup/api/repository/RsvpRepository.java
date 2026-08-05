package com.showup.api.repository;

import com.showup.api.entity.Rsvp;
import com.showup.api.enums.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RsvpRepository extends JpaRepository<Rsvp, UUID> {

    Optional<Rsvp> findByEventIdAndMemberId(UUID eventId, UUID memberId);

    List<Rsvp> findAllByEventIdAndStatus(UUID eventId, RsvpStatus status);

    List<Rsvp> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);

    long countByEventIdAndStatus(UUID eventId, RsvpStatus status);

    /** Head of the waitlist — the next RSVP to promote when seats free up. */
    Optional<Rsvp> findFirstByEventIdAndStatusOrderByWaitlistPositionAsc(UUID eventId, RsvpStatus status);

    @Query("select coalesce(max(r.waitlistPosition), 0) from Rsvp r "
            + "where r.event.id = :eventId and r.status = com.showup.api.enums.RsvpStatus.WAITLISTED")
    int findMaxWaitlistPosition(UUID eventId);
}
