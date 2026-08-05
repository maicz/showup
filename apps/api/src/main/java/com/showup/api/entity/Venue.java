package com.showup.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.locationtech.jts.geom.Point;

/**
 * Reusable across events, so a group's regular space is entered once. See
 * docs/domain-model.md#venue.
 */
@Entity
@Table(name = "venue")
public class Venue extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Embedded
    private Address address;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(columnDefinition = "text")
    private String notes;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_group_id", nullable = false, foreignKey = @ForeignKey(name = "fk_venue_group"))
    private Group createdByGroup;

    protected Venue() {
        // for JPA
    }

    public Venue(String name, Address address, Point location, String notes, Group createdByGroup) {
        this.name = name;
        this.address = address;
        this.location = location;
        this.notes = notes;
        this.createdByGroup = createdByGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Group getCreatedByGroup() {
        return createdByGroup;
    }
}
