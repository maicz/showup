package com.showup.api.repository;

import com.showup.api.dto.RsvpRollup;
import com.showup.api.entity.Rsvp;
import com.showup.api.enums.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    @Query("""
            select new com.showup.api.dto.RsvpRollup(count(r), coalesce(sum(1 + r.guestCount), 0))
              from Rsvp r
             where r.event.group.id = :groupId
               and r.event.startsAt between :from and :to
               and r.status = :status
            """)
    RsvpRollup aggregateRsvpsForGroupBetween(
            @Param("groupId") UUID groupId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("status") RsvpStatus status);
}
