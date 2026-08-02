package com.showup.api.member;

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

/** Join table: a member following a {@link Topic}. Unique on the pair. */
@Entity
@Table(name = "member_interest", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_interest_member_topic", columnNames = {"member_id", "topic_id"})
})
public class MemberInterest extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_interest_member"))
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_interest_topic"))
    private Topic topic;

    protected MemberInterest() {
        // for JPA
    }

    public MemberInterest(Member member, Topic topic) {
        this.member = member;
        this.topic = topic;
    }

    public Member getMember() {
        return member;
    }

    public Topic getTopic() {
        return topic;
    }
}
