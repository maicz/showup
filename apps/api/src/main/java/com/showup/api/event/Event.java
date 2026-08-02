package com.showup.api.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Event {

    @Id
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    private String description;

    @NotBlank
    @Column(nullable = false)
    private String location;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Positive
    @Column(nullable = false)
    private int capacity;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Event() {
        // for JPA
    }

    public Event(String title, String description, String location, Instant startsAt, int capacity) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.location = location;
        this.startsAt = startsAt;
        this.capacity = capacity;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public int getCapacity() {
        return capacity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
