package com.showup.api.group;

import com.showup.api.shared.BaseEntity;
import com.showup.api.topic.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code group} is a reserved SQL word, so the table is named {@code meetup_group} to avoid a
 * permanent quoting tax in every hand-written query. See docs/domain-model.md#group.
 */
@Entity
@Table(name = "meetup_group")
public class Group extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true, length = 80)
    private String urlname;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_group_category"))
    private Category category;

    private String city;

    private String country;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point location;

    @NotBlank
    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupVisibility visibility;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "join_policy", nullable = false, length = 20)
    private GroupJoinPolicy joinPolicy;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Column(name = "rating_average", precision = 2, scale = 1)
    private BigDecimal ratingAverage;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "founded_at")
    private Instant foundedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupStatus status;

    protected Group() {
        // for JPA
    }

    public Group(String urlname, String name, Category category, String timeZone) {
        this.urlname = urlname;
        this.name = name;
        this.category = category;
        this.timeZone = timeZone;
        this.visibility = GroupVisibility.PUBLIC;
        this.joinPolicy = GroupJoinPolicy.OPEN;
        this.status = GroupStatus.ACTIVE;
        this.memberCount = 0;
        this.ratingCount = 0;
        this.foundedAt = Instant.now();
    }

    public String getUrlname() {
        return urlname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public GroupVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(GroupVisibility visibility) {
        this.visibility = visibility;
    }

    public GroupJoinPolicy getJoinPolicy() {
        return joinPolicy;
    }

    public void setJoinPolicy(GroupJoinPolicy joinPolicy) {
        this.joinPolicy = joinPolicy;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(BigDecimal ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Instant getFoundedAt() {
        return foundedAt;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public void setStatus(GroupStatus status) {
        this.status = status;
    }
}
