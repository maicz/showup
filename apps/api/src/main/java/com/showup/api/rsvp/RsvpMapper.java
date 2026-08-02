package com.showup.api.rsvp;

import com.showup.api.member.MemberMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MemberMapper.class)
public interface RsvpMapper {

    RsvpSummary toSummary(Rsvp rsvp);

    @Mapping(target = "member", source = "member")
    AttendeeSummary toAttendeeSummary(Rsvp rsvp);
}
