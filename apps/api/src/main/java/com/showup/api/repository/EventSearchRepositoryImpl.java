package com.showup.api.repository;

import com.showup.api.dto.EventSearchQuery;
import com.showup.api.entity.Event;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Native SQL rather than Criteria: the radius filter is {@code ST_DWithin} over a
 * {@code geography} column, which has no portable Criteria spelling, and the availability filter
 * compares {@code yes_rsvp_count} against {@code capacity}.
 *
 * <p>Only the <em>presence</em> of each clause is dynamic — every value is bound as a named
 * parameter, and {@code sort} is resolved through a fixed whitelist, so no caller input reaches
 * the SQL text.
 */
public class EventSearchRepositoryImpl implements EventSearchRepository {

    /** Whitelist: anything not in here falls back to the default ordering. */
    private static final Map<String, String> SORTS = Map.of(
            "startsAt", "e.starts_at asc",
            "-startsAt", "e.starts_at desc",
            "fee", "e.fee_amount_minor asc",
            "-fee", "e.fee_amount_minor desc",
            "popularity", "e.yes_rsvp_count desc",
            "newest", "e.created_at desc");

    private static final String DEFAULT_SORT = "e.starts_at asc";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Event> search(EventSearchQuery query) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();

        // Browse only ever shows published events; drafts and cancellations are not public.
        where.add("e.status = 'PUBLISHED'");

        boolean joinVenue = query.lat() != null && query.lon() != null && query.radiusKm() != null;
        boolean joinGroup = query.categorySlug() != null
                || query.topicSlugs() != null && !query.topicSlugs().isEmpty()
                || query.query() != null;

        if (joinVenue) {
            where.add("""
                    v.location is not null and ST_DWithin(
                        v.location,
                        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                        :radiusMeters)""");
            params.put("lon", query.lon());
            params.put("lat", query.lat());
            params.put("radiusMeters", query.radiusKm() * 1000.0);
        }
        if (query.categorySlug() != null) {
            where.add("c.slug = :categorySlug");
            params.put("categorySlug", query.categorySlug());
        }
        if (query.topicSlugs() != null && !query.topicSlugs().isEmpty()) {
            where.add("""
                    exists (select 1 from group_topic gt
                              join topic t on t.id = gt.topic_id
                             where gt.group_id = g.id and t.slug in (:topicSlugs))""");
            params.put("topicSlugs", query.topicSlugs());
        }
        if (query.format() != null) {
            where.add("e.format = :format");
            params.put("format", query.format().name());
        }
        if (query.dateFrom() != null) {
            where.add("e.starts_at >= :dateFrom");
            params.put("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            where.add("e.starts_at <= :dateTo");
            params.put("dateTo", query.dateTo());
        }
        if (query.maxFee() != null) {
            where.add("e.fee_amount_minor <= :maxFee");
            params.put("maxFee", query.maxFee());
        }
        if (query.availability() != null) {
            where.add(switch (query.availability()) {
                case SEATS_AVAILABLE -> "(e.capacity is null or e.yes_rsvp_count < e.capacity)";
                case WAITLIST -> "(e.capacity is not null and e.yes_rsvp_count >= e.capacity and e.waitlist_enabled)";
                case FULL -> "(e.capacity is not null and e.yes_rsvp_count >= e.capacity and not e.waitlist_enabled)";
            });
        }
        if (query.query() != null) {
            where.add("(lower(e.title) like :searchLike or lower(g.name) like :searchLike)");
            params.put("searchLike", "%" + query.query().toLowerCase() + "%");
        }

        String joins = (joinVenue ? " left join venue v on v.id = e.venue_id" : "")
                + (joinGroup ? " join meetup_group g on g.id = e.group_id join category c on c.id = g.category_id" : "");
        String predicate = String.join(" and ", where);

        // Paging is already normalized and capped by the EventSearchQuery constructor.
        int page = query.page();
        int size = query.size();
        // Map.of rejects a null key outright, and sort is optional.
        String order = query.sort() == null ? DEFAULT_SORT : SORTS.getOrDefault(query.sort(), DEFAULT_SORT);

        Query total = entityManager.createNativeQuery(
                "select count(*) from event e" + joins + " where " + predicate);
        params.forEach(total::setParameter);
        long count = ((Number) total.getSingleResult()).longValue();
        if (count == 0) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        }

        Query rows = entityManager.createNativeQuery(
                "select e.id from event e" + joins + " where " + predicate + " order by " + order);
        params.forEach(rows::setParameter);

        @SuppressWarnings("unchecked")
        List<?> rawIds = rows
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        if (rawIds.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), count);
        }

        List<UUID> ids = rawIds.stream()
                .map(obj -> obj instanceof UUID u ? u : UUID.fromString(obj.toString()))
                .toList();

        Map<UUID, Event> byId = entityManager.createQuery("""
                select e from Event e
                 join fetch e.group g
                 join fetch g.category
                 left join fetch e.venue
                where e.id in :ids
                """, Event.class)
                .setParameter("ids", ids)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        List<Event> content = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(content, PageRequest.of(page, size), count);
    }
}
