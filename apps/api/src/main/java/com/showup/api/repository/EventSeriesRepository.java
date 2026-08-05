package com.showup.api.repository;

import com.showup.api.entity.EventSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventSeriesRepository extends JpaRepository<EventSeries, UUID> {

    List<EventSeries> findAllByGroupId(UUID groupId);
}
