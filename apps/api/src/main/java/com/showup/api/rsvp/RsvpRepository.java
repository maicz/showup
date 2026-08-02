package com.showup.api.rsvp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RsvpRepository extends JpaRepository<Rsvp, UUID> {

    Optional<Rsvp> findByEventIdAndMemberId(UUID eventId, UUID memberId);

    List<Rsvp> findAllByEventIdAndStatus(UUID eventId, RsvpStatus status);

    List<Rsvp> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);
}
