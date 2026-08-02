package com.showup.api.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberIdentityRepository extends JpaRepository<MemberIdentity, UUID> {

    Optional<MemberIdentity> findByProviderAndSubject(IdentityProvider provider, String subject);

    java.util.List<MemberIdentity> findAllByMemberId(UUID memberId);
}
