package com.showup.api.repository;

import com.showup.api.entity.Group;
import com.showup.api.enums.GroupStatus;
import com.showup.api.enums.GroupVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findByUrlname(String urlname);

    boolean existsByUrlname(String urlname);

    @EntityGraph(attributePaths = {"category"})
    @Query("""
            select g from Group g
             where g.visibility = :visibility
               and g.status = :status
               and (:query is null or lower(g.name) like concat('%', lower(:query), '%'))
               and (:categorySlug is null or g.category.slug = :categorySlug)
               and (:city is null or lower(g.city) = :city)
            """)
    Page<Group> searchPublic(
            @Param("visibility") GroupVisibility visibility,
            @Param("status") GroupStatus status,
            @Param("query") String query,
            @Param("categorySlug") String categorySlug,
            @Param("city") String city,
            Pageable pageable);
}
