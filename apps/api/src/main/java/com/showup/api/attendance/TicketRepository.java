package com.showup.api.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByCode(String code);

    Optional<Ticket> findByRsvpId(UUID rsvpId);
}
