package com.showup.api.repository;

import com.showup.api.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findByUrlname(String urlname);

    boolean existsByUrlname(String urlname);
}
