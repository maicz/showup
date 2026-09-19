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
               and (cast(:query as string) is null
                    or lower(g.name) like concat('%', lower(cast(:query as string)), '%'))
               and (cast(:categorySlug as string) is null
                    or g.category.slug = cast(:categorySlug as string))
               and (cast(:city as string) is null
                    or lower(g.city) = cast(:city as string))
            """)
    Page<Group> searchPublic(
            @Param("visibility") GroupVisibility visibility,
            @Param("status") GroupStatus status,
            @Param("query") String query,
            @Param("categorySlug") String categorySlug,
            @Param("city") String city,
            Pageable pageable);
}
