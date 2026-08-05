package com.showup.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * One of Meetup's 24 broad browse categories. See docs/domain-model.md#category-and-topic.
 */
@Entity
@Table(name = "category")
public class Category extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String slug;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Category() {
        // for JPA
    }

    public Category(String slug, String name, String iconUrl, int displayOrder) {
        this.slug = slug;
        this.name = name;
        this.iconUrl = iconUrl;
        this.displayOrder = displayOrder;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
