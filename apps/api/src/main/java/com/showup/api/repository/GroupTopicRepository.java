package com.showup.api.repository;

import com.showup.api.entity.GroupTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupTopicRepository extends JpaRepository<GroupTopic, UUID> {

    List<GroupTopic> findAllByGroupId(UUID groupId);

    List<GroupTopic> findAllByGroupIdIn(List<UUID> groupIds);

    void deleteAllByGroupId(UUID groupId);
}
