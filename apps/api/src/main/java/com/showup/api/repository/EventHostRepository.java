package com.showup.api.repository;

import com.showup.api.entity.EventHost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventHostRepository extends JpaRepository<EventHost, UUID> {

    List<EventHost> findAllByEventId(UUID eventId);

    Optional<EventHost> findByEventIdAndMemberId(UUID eventId, UUID memberId);

    boolean existsByEventIdAndMemberId(UUID eventId, UUID memberId);
}
