package com.showup.api.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroupTopicRepository extends JpaRepository<GroupTopic, UUID> {

    List<GroupTopic> findAllByGroupId(UUID groupId);

    void deleteAllByGroupId(UUID groupId);
}
