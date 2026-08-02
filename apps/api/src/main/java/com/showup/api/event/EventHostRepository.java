package com.showup.api.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventHostRepository extends JpaRepository<EventHost, UUID> {

    List<EventHost> findAllByEventId(UUID eventId);
}
