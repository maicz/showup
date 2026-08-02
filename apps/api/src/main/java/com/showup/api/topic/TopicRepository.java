package com.showup.api.topic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findAllByCategoryId(UUID categoryId);

    Optional<Topic> findBySlug(String slug);

    List<Topic> findAllBySlugIn(List<String> slugs);
}
