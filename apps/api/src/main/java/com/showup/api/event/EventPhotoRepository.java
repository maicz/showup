package com.showup.api.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventPhotoRepository extends JpaRepository<EventPhoto, UUID> {

    List<EventPhoto> findAllByEventId(UUID eventId);
}
