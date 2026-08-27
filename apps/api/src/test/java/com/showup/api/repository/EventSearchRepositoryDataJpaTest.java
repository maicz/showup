package com.showup.api.repository;

import com.showup.api.config.TestcontainersConfiguration;
import com.showup.api.dto.EventSearchQuery;
import com.showup.api.dto.GeoPoint;
import com.showup.api.entity.Address;
import com.showup.api.entity.Category;
import com.showup.api.entity.Event;
import com.showup.api.entity.Group;
import com.showup.api.entity.GroupTopic;
import com.showup.api.entity.Money;
import com.showup.api.entity.Topic;
import com.showup.api.entity.Venue;
import com.showup.api.enums.AvailabilityState;
import com.showup.api.enums.EventFormat;
import com.showup.api.enums.EventStatus;
import com.showup.api.util.GeoPoints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@code @DataJpaTest} slice: only the JPA layer boots — entity manager, repositories, and
 * Flyway against the real Testcontainers Postgres/PostGIS instance — with no web layer, no
 * security filter chain, and no service beans at all. {@link AutoConfigureTestDatabase.Replace#NONE}
 * keeps Boot from swapping in an embedded database, which would not understand the
 * {@code geography(Point)} columns or {@code ST_DWithin} that this native query relies on.
 *
 * <p>{@link EventSearchRepositoryImpl} is native SQL precisely because the radius filter and the
 * availability comparison have no portable Criteria or derived-query spelling — see the class
 * comment there — so its branches (which joins get added, which predicates, the sort whitelist)
 * are far more directly tested by building fixtures with {@link TestEntityManager} and calling the
 * repository than by going through HTTP and a whole search DTO round trip.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class EventSearchRepositoryDataJpaTest {

    private static final GeoPoint BUCHAREST = new GeoPoint(44.4268, 26.1025);
    private static final GeoPoint TOKYO = new GeoPoint(35.6762, 139.6503);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository events;

    private Category category;
    private Topic topic;

    @Test
    void onlyPublishedEventsAreEverReturned() {
        Group g = persistGroup();
        Event draft = persistEvent(g, null, EventStatus.DRAFT, EventFormat.IN_PERSON, 0, null, false, Instant.now());
        Event published = persistEvent(g, null, EventStatus.PUBLISHED, EventFormat.IN_PERSON, 0, null,
                false, Instant.now());
        entityManager.flush();

        List<UUID> ids = idsFor(query(null, null, null, null, null, null, null, null, null, null));

        assertThat(ids).contains(published.getId()).doesNotContain(draft.getId());
    }

    @Test
    void theRadiusFilterIncludesNearbyVenuesAndExcludesFarOnes() {
        Group g = persistGroup();
        Venue near = persistVenue(g, BUCHAREST);
        Venue far = persistVenue(g, TOKYO);
        Event nearEvent = persistEvent(g, near, EventStatus.PUBLISHED, EventFormat.IN_PERSON, 0, null,
                false, Instant.now().plus(1, ChronoUnit.DAYS));
        Event farEvent = persistEvent(g, far, EventStatus.PUBLISHED, EventFormat.IN_PERSON, 0, null, false,
                Instant.now().plus(1, ChronoUnit.DAYS));
        entityManager.flush();

        EventSearchQuery searchQuery = new EventSearchQuery(BUCHAREST.latitude(), BUCHAREST.longitude(), 25.0,
                null, null, null, null, null, null, null, null, null, null, null);

        assertThat(idsFor(searchQuery)).contains(nearEvent.getId()).doesNotContain(farEvent.getId());
    }

    @Test
    void categorySlugAndTopicSlugsFilterThroughTheGroupJoin() {
        Group inCategory = persistGroup();
        Group otherCategory = persistGroupWithOwnCategory();
        Event matching = persistEvent(inCategory, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0, null,
                false, Instant.now().plus(1, ChronoUnit.DAYS));
        Event other = persistEvent(otherCategory, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0, null, false,
                Instant.now().plus(1, ChronoUnit.DAYS));
        entityManager.flush();

        List<UUID> byCategory = idsFor(query(null, null, null, category.getSlug(), null, null, null, null, null, null));
        assertThat(byCategory).contains(matching.getId()).doesNotContain(other.getId());

        List<UUID> byTopic = idsFor(
                query(null, null, null, null, List.of(topic.getSlug()), null, null, null, null, null));
        assertThat(byTopic).contains(matching.getId()).doesNotContain(other.getId());

        List<UUID> byUnknownTopic = idsFor(
                query(null, null, null, null, List.of("no-such-topic"), null, null, null, null, null));
        assertThat(byUnknownTopic).doesNotContain(matching.getId(), other.getId());
    }

    @Test
    void dateRangeAndFormatAndMaxFeeEachNarrowTheResults() {
        Group g = persistGroup();
        Instant soon = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant later = Instant.now().plus(40, ChronoUnit.DAYS);
        Event cheapSoon = persistEvent(g, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0, null, false, soon);
        Event pricedLater = persistEvent(g, null, EventStatus.PUBLISHED, EventFormat.IN_PERSON, 5000, null,
                false, later);
        entityManager.flush();

        assertThat(idsFor(query(null, null, null, null, null, EventFormat.ONLINE, null, null, null, null)))
                .contains(cheapSoon.getId()).doesNotContain(pricedLater.getId());

        assertThat(idsFor(query(null, null, null, null, null, null, null, null, 100L, null)))
                .contains(cheapSoon.getId()).doesNotContain(pricedLater.getId());

        assertThat(idsFor(query(null, null, null, null, null, null, soon.minusSeconds(1),
                soon.plusSeconds(1), null, null))).contains(cheapSoon.getId()).doesNotContain(pricedLater.getId());

        assertThat(idsFor(query(null, null, null, null, null, null, later.minusSeconds(1), null, null, null)))
                .contains(pricedLater.getId()).doesNotContain(cheapSoon.getId());
    }

    @Test
    void availabilityReflectsCapacityAgainstSeatedCountAndTheWaitlistFlag() {
        Group g = persistGroup();
        Instant starts = Instant.now().plus(1, ChronoUnit.DAYS);
        Event available = persistEvent(g, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0, 5, false, starts);
        available.setYesRsvpCount(2);
        Event waitlisted = persistEvent(g, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0, 2, true, starts);
        waitlisted.setYesRsvpCount(2);
        Event full = persistEvent(g, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0, 2, false, starts);
        full.setYesRsvpCount(2);
        entityManager.flush();

        List<UUID> ours = List.of(available.getId(), waitlisted.getId(), full.getId());
        assertThat(idsFor(AvailabilityState.SEATS_AVAILABLE)).contains(available.getId())
                .doesNotContain(waitlisted.getId(), full.getId());
        assertThat(idsFor(AvailabilityState.WAITLIST)).contains(waitlisted.getId())
                .doesNotContain(available.getId(), full.getId());
        assertThat(idsFor(AvailabilityState.FULL)).contains(full.getId())
                .doesNotContain(available.getId(), waitlisted.getId());
        assertThat(ours).hasSize(3); // sanity: the three fixtures really are distinct rows
    }

    @Test
    void anUnknownSortValueFallsBackToTheDefaultRatherThanFailing() {
        Group g = persistGroup();
        Instant first = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant second = Instant.now().plus(2, ChronoUnit.DAYS);
        Event earlier = persistEvent(g, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0, null, false, first);
        Event later = persistEvent(g, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0, null, false, second);
        entityManager.flush();

        EventSearchQuery searchQuery = new EventSearchQuery(null, null, null, null, null, null, null, null, null,
                null, null, null, null, "not-a-real-sort-key");
        List<UUID> ours = events.search(searchQuery).getContent().stream()
                .map(Event::getId)
                .filter(id -> id.equals(earlier.getId()) || id.equals(later.getId()))
                .toList();

        // Order among our own two fixtures must still be startsAt ascending — the default sort —
        // regardless of whatever else the shared database happens to contain.
        assertThat(ours).containsExactly(earlier.getId(), later.getId());
    }

    @Test
    void keywordSearchMatchesEventOrGroupNamesWithoutBeingCaseSensitive() {
        Group matchingGroup = persistGroup();
        Group otherGroup = persistGroupWithOwnCategory();
        Event titleMatch = persistEvent(matchingGroup, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0,
                null, false, Instant.now().plus(1, ChronoUnit.DAYS));
        titleMatch.setTitle("Kotlin Coroutines Workshop");
        Event other = persistEvent(otherGroup, null, EventStatus.PUBLISHED, EventFormat.ONLINE, 0,
                null, false, Instant.now().plus(2, ChronoUnit.DAYS));
        other.setTitle("Unrelated Gathering");
        entityManager.flush();

        EventSearchQuery byTitle = new EventSearchQuery(null, null, null, null, null, null, null, null,
                null, null, "  COROUTINES ", null, null, null);
        EventSearchQuery byGroup = new EventSearchQuery(null, null, null, null, null, null, null, null,
                null, null, matchingGroup.getName().toUpperCase(), null, null, null);

        assertThat(idsFor(byTitle)).contains(titleMatch.getId()).doesNotContain(other.getId());
        assertThat(idsFor(byGroup)).contains(titleMatch.getId()).doesNotContain(other.getId());
    }

    // --- fixtures ---

    private Group persistGroup() {
        if (category == null) {
            category = entityManager.persist(new Category("search-cat-" + UUID.randomUUID(), "Search Category",
                    null, 1));
            topic = entityManager.persist(new Topic(category, "search-topic-" + UUID.randomUUID(), "Search Topic"));
        }
        Group group = new Group("search-group-" + UUID.randomUUID(), "Search Group", category, "Europe/Bucharest");
        entityManager.persist(group);
        entityManager.persist(new GroupTopic(group, topic));
        return group;
    }

    private Group persistGroupWithOwnCategory() {
        Category otherCategory = entityManager.persist(new Category("other-cat-" + UUID.randomUUID(),
                "Other Category", null, 2));
        Group other = new Group("other-group-" + UUID.randomUUID(), "Other Group", otherCategory,
                "Europe/Bucharest");
        entityManager.persist(other);
        return other;
    }

    private Venue persistVenue(Group owner, GeoPoint at) {
        Venue venue = new Venue("Venue", new Address("Line 1", null, "City", null, null, "RO"),
                GeoPoints.toJts(at), null, owner);
        return entityManager.persist(venue);
    }

    private Event persistEvent(Group owner, Venue venue, EventStatus status, EventFormat format,
                               long feeAmountMinor, Integer capacity, boolean waitlistEnabled, Instant startsAt) {
        Event event = new Event(owner, "Search Event " + UUID.randomUUID(), format, startsAt, "UTC",
                new Money(feeAmountMinor, "USD"));
        event.setStatus(status);
        event.setVenue(venue);
        event.setCapacity(capacity);
        event.setWaitlistEnabled(waitlistEnabled);
        return entityManager.persist(event);
    }

    private EventSearchQuery query(Double lat, Double lon, Double radiusKm, String categorySlug,
                                   List<String> topicSlugs, EventFormat format, Instant dateFrom, Instant dateTo,
                                   Long maxFee, AvailabilityState availability) {
        return new EventSearchQuery(lat, lon, radiusKm, categorySlug, topicSlugs, format, dateFrom, dateTo,
                maxFee, availability, null, null, null, null);
    }

    private List<UUID> idsFor(AvailabilityState availability) {
        return idsFor(query(null, null, null, null, null, null, null, null, null, availability));
    }

    private List<UUID> idsFor(EventSearchQuery searchQuery) {
        return events.search(searchQuery).getContent().stream().map(Event::getId).toList();
    }
}
