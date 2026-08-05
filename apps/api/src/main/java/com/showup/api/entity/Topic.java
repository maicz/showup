package com.showup.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A fine-grained tag under a {@link Category}, used for group tagging and member matching.
 * See docs/domain-model.md#category-and-topic.
 */
@Entity
@Table(name = "topic")
public class Topic extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String slug;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "group_count", nullable = false)
    private int groupCount;

    protected Topic() {
        // for JPA
    }

    public Topic(Category category, String slug, String name) {
        this.category = category;
        this.slug = slug;
        this.name = name;
        this.groupCount = 0;
    }

    public Category getCategory() {
        return category;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }
}
