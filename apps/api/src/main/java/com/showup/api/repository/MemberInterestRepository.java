package com.showup.api.repository;

import com.showup.api.entity.MemberInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemberInterestRepository extends JpaRepository<MemberInterest, UUID> {

    List<MemberInterest> findAllByMemberId(UUID memberId);

    void deleteAllByMemberId(UUID memberId);
}
