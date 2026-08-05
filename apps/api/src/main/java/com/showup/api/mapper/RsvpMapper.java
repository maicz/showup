package com.showup.api.mapper;

import com.showup.api.dto.AttendeeSummary;
import com.showup.api.dto.RsvpSummary;
import com.showup.api.entity.Rsvp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MemberMapper.class)
public interface RsvpMapper {

    RsvpSummary toSummary(Rsvp rsvp);

    @Mapping(target = "member", source = "member")
    AttendeeSummary toAttendeeSummary(Rsvp rsvp);
}
