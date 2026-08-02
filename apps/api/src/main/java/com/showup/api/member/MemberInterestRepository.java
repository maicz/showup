package com.showup.api.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberInterestRepository extends JpaRepository<MemberInterest, UUID> {

    List<MemberInterest> findAllByMemberId(UUID memberId);

    void deleteAllByMemberId(UUID memberId);
}
