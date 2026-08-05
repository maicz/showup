package com.showup.api.repository;

import com.showup.api.entity.MemberIdentity;
import com.showup.api.enums.IdentityProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberIdentityRepository extends JpaRepository<MemberIdentity, UUID> {

    Optional<MemberIdentity> findByProviderAndSubject(IdentityProvider provider, String subject);

    java.util.List<MemberIdentity> findAllByMemberId(UUID memberId);
}
