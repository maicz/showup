package com.showup.api.repository;

import com.showup.api.entity.Event;
import com.showup.api.enums.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, EventSearchRepository {

    List<Event> findAllByGroupIdOrderByStartsAtDesc(UUID groupId);

    List<Event> findAllByStatusOrderByStartsAtAsc(EventStatus status);

    List<Event> findAllByGroupIdAndStatusOrderByStartsAtAsc(UUID groupId, EventStatus status);

    List<Event> findAllBySeriesIdOrderByStartsAtAsc(UUID seriesId);

    List<Event> findAllByGroupIdAndStartsAtBetween(UUID groupId, Instant from, Instant to);

    /**
     * Takes {@code SELECT ... FOR UPDATE} on the event row. Seat allocation reads
     * {@code yesRsvpCount} and writes it back; without the lock two concurrent RSVPs both read
     * the pre-increment count and the event oversells. See docs/domain-model.md#rsvp.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(UUID id);
}
