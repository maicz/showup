package com.showup.api.repository;

import com.showup.api.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    Optional<CheckIn> findByTicketId(UUID ticketId);

    /** Every check-in for an event, walked through ticket -> rsvp -> event. */
    List<CheckIn> findAllByTicketRsvpEventId(UUID eventId);

    long countByTicketRsvpEventId(UUID eventId);

    long countByTicketRsvpEventGroupIdAndCheckedInAtBetween(UUID groupId, java.time.Instant from, java.time.Instant to);

    @Query("""
            select coalesce(sum(c.admittedCount), 0)
              from CheckIn c
             where c.ticket.rsvp.event.group.id = :groupId
               and c.ticket.rsvp.event.startsAt between :from and :to
            """)
    long sumAdmittedCountForGroupBetween(
            @Param("groupId") UUID groupId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
