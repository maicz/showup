package com.showup.api.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventSeriesRepository extends JpaRepository<EventSeries, UUID> {

    List<EventSeries> findAllByGroupId(UUID groupId);
}
