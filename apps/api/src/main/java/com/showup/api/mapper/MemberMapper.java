package com.showup.api.mapper;

import com.showup.api.dto.MemberProfile;
import com.showup.api.dto.MemberSummary;
import com.showup.api.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@link Member#getPasswordHash()} is intentionally never referenced here — entities never
 * cross the controller boundary, and a mapped {@code passwordHash} field is exactly the kind
 * of forgotten-annotation credential leak that rule exists to prevent.
 */
@Mapper(componentModel = "spring")
public interface MemberMapper {

    MemberSummary toSummary(Member member);

    @Mapping(target = "memberSince", source = "createdAt")
    @Mapping(target = "emailVerified", expression = "java(member.getEmailVerifiedAt() != null)")
    MemberProfile toProfile(Member member);
}
