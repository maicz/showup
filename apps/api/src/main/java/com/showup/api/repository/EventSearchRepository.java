package com.showup.api.repository;

import com.showup.api.dto.EventSearchQuery;
import com.showup.api.entity.Event;
import org.springframework.data.domain.Page;

/**
 * The faceted browse query, which derived query methods cannot express: the radius filter needs
 * PostGIS {@code ST_DWithin} and the availability filter compares two columns. Implemented in
 * {@link EventSearchRepositoryImpl} and mixed into {@link EventRepository}.
 */
public interface EventSearchRepository {

    Page<Event> search(EventSearchQuery query);
}
