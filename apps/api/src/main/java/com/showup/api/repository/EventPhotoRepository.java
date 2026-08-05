package com.showup.api.repository;

import com.showup.api.entity.EventPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventPhotoRepository extends JpaRepository<EventPhoto, UUID> {

    List<EventPhoto> findAllByEventId(UUID eventId);
}
