package com.showup.api.group;

import com.showup.api.shared.BaseEntity;
import com.showup.api.topic.Topic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/** Join table: a {@link Group} tagged with a {@link Topic}. Unique on the pair. */
@Entity
@Table(name = "group_topic", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_topic_group_topic", columnNames = {"group_id", "topic_id"})
})
public class GroupTopic extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(name = "fk_group_topic_group"))
    private Group group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false, foreignKey = @ForeignKey(name = "fk_group_topic_topic"))
    private Topic topic;

    protected GroupTopic() {
        // for JPA
    }

    public GroupTopic(Group group, Topic topic) {
        this.group = group;
        this.topic = topic;
    }

    public Group getGroup() {
        return group;
    }

    public Topic getTopic() {
        return topic;
    }
}
