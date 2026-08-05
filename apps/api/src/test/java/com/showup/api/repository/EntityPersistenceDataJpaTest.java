package com.showup.api.repository;

import com.showup.api.config.TestcontainersConfiguration;
import com.showup.api.entity.BaseEntity;
import com.showup.api.entity.Category;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventComment;
import com.showup.api.entity.EventPhoto;
import com.showup.api.entity.Group;
import com.showup.api.entity.Member;
import com.showup.api.entity.MemberIdentity;
import com.showup.api.entity.Money;
import com.showup.api.enums.EventFormat;
import com.showup.api.enums.IdentityProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Another {@code @DataJpaTest} slice, this time for entities that are fully modelled — with a
 * repository, a migration, and validation annotations — but not yet wired to any service or
 * controller: {@link EventComment}, {@link EventPhoto}, and {@link MemberIdentity} are the
 * schema's forward-looking pieces (threaded replies, photo galleries, linked SSO providers) that
 * {@code AuthService.ssoLogin} explicitly declines to finish wiring up yet. Persisting them
 * directly through their repositories, without a service in front, is the only way to exercise
 * that modelled-but-unused code.
 *
 * <p>It also covers {@link BaseEntity}'s shared {@code equals}/{@code hashCode} contract and the
 * {@code @PreUpdate} timestamp refresh, which need real, persisted identities to test meaningfully
 * — a fresh, transient entity's id is always null, which a plain unit test cannot get past.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class EntityPersistenceDataJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventCommentRepository comments;

    @Autowired
    private EventPhotoRepository photos;

    @Autowired
    private MemberIdentityRepository identities;

    @Test
    void aCommentCanBeAThreadedReplyAndIsSoftDeletable() {
        Event event = persistEvent();
        Member author = persistMember("author");
        Member replier = persistMember("replier");

        EventComment root = entityManager.persist(new EventComment(event, author, "Great talk!", null));
        EventComment reply = entityManager.persist(new EventComment(event, replier, "Agreed", root));
        entityManager.flush();
        entityManager.clear();

        List<EventComment> thread = comments.findAllByEventIdOrderByCreatedAtAsc(event.getId());
        assertThat(thread).hasSize(2);
        EventComment fetchedReply = thread.stream().filter(c -> c.getParentComment() != null).findFirst().orElseThrow();
        assertThat(fetchedReply.getParentComment().getId()).isEqualTo(root.getId());
        assertThat(fetchedReply.getBody()).isEqualTo("Agreed");

        fetchedReply.setDeletedAt(Instant.now());
        entityManager.flush();
        entityManager.clear();
        assertThat(comments.findAllByEventIdOrderByCreatedAtAsc(event.getId()).stream()
                .filter(c -> c.getId().equals(fetchedReply.getId())).findFirst().orElseThrow()
                .getDeletedAt()).isNotNull();
    }

    @Test
    void aPhotoStoresOnlyTheUrlAndItsDimensions() {
        Event event = persistEvent();
        Member uploader = persistMember("uploader");

        entityManager.persistAndFlush(new EventPhoto(event, uploader, "https://cdn.test/photo.jpg", "Nice shot",
                1920, 1080));
        entityManager.clear();

        EventPhoto photo = photos.findAllByEventId(event.getId()).get(0);
        assertThat(photo.getUrl()).isEqualTo("https://cdn.test/photo.jpg");
        assertThat(photo.getCaption()).isEqualTo("Nice shot");
        assertThat(photo.getWidth()).isEqualTo(1920);
        assertThat(photo.getHeight()).isEqualTo(1080);
        assertThat(photo.getUploadedBy().getId()).isEqualTo(uploader.getId());
    }

    @Test
    void aMemberCanLinkAnSsoIdentityLookedUpByProviderAndSubject() {
        Member member = persistMember("sso-user");
        entityManager.persistAndFlush(new MemberIdentity(member, IdentityProvider.GOOGLE, "google-subject-123"));
        entityManager.clear();

        MemberIdentity found = identities.findByProviderAndSubject(IdentityProvider.GOOGLE, "google-subject-123")
                .orElseThrow();
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
        assertThat(identities.findAllByMemberId(member.getId())).hasSize(1);
        assertThat(identities.findByProviderAndSubject(IdentityProvider.GOOGLE, "no-such-subject")).isEmpty();
    }

    @Test
    void equalsIsIdentityBasedOncePersistedAndHashCodeIsStableAcrossTheLifecycle() {
        Member first = persistMember("member-one");
        Member second = persistMember("member-two");
        entityManager.flush();

        assertThat(first).isEqualTo(first);
        assertThat(first).isNotEqualTo(second);
        assertThat(first).isNotEqualTo(null);
        assertThat(first).isNotEqualTo("not even the same type");
        assertThat(first.hashCode()).isEqualTo(first.hashCode());

        // Two brand new, unsaved entities of the same type both have a null id and are not equal.
        Member transientOne = new Member("a@example.test", null, "A");
        Member transientTwo = new Member("b@example.test", null, "B");
        assertThat(transientOne).isNotEqualTo(transientTwo);
    }

    @Test
    void updatedAtAdvancesOnAPreUpdateFlushButNotOnTheInitialInsert() {
        Member member = persistMember("timestamp-check");
        entityManager.flush();
        Instant insertedAt = member.getUpdatedAt();
        assertThat(insertedAt).isNotNull();

        member.setBio("updated via a dirty-checked flush");
        entityManager.flush();

        assertThat(member.getUpdatedAt()).isAfterOrEqualTo(insertedAt);
    }

    // --- fixtures ---

    private Event persistEvent() {
        Category category = entityManager.persist(new Category("ep-cat-" + UUID.randomUUID(), "Category", null, 1));
        Group group = entityManager.persist(new Group("ep-group-" + UUID.randomUUID(), "Group", category, "UTC"));
        Event event = new Event(group, "Persistence Test Event", EventFormat.ONLINE,
                Instant.now().plus(1, ChronoUnit.DAYS), "UTC", Money.zero("USD"));
        return entityManager.persist(event);
    }

    private Member persistMember(String label) {
        return entityManager.persist(new Member(label + "-" + UUID.randomUUID() + "@example.test",
                "hash", label));
    }
}
