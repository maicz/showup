package com.showup.api.repository;

import com.showup.api.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByCode(String code);

    Optional<Ticket> findByRsvpId(UUID rsvpId);
}
