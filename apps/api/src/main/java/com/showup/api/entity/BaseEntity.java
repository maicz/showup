package com.showup.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Audit columns shared by every table: {@code id}/{@code created_at} are assigned by the
 * database default (uuidv7()/now()) and never touched again, {@code updated_at} gets the same
 * insert default and is then refreshed on every write, and {@code version} backs optimistic
 * locking. See docs/domain-model.md#entities.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @ColumnDefault("uuidv7()")
    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @ColumnDefault("now()")
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @ColumnDefault("now()")
    @Generated(event = EventType.INSERT)
    @Column(name = "updated_at", insertable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
