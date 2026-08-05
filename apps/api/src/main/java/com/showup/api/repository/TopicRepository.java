package com.showup.api.repository;

import com.showup.api.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findAllByCategoryId(UUID categoryId);

    Optional<Topic> findBySlug(String slug);

    List<Topic> findAllBySlugIn(List<String> slugs);
}
