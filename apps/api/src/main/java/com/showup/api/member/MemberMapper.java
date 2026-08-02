package com.showup.api.member;

import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

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
