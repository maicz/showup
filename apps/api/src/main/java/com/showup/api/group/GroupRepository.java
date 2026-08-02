package com.showup.api.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findByUrlname(String urlname);

    boolean existsByUrlname(String urlname);
}
