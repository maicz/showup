package com.showup.api.attendance;

import com.showup.api.member.MemberMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MemberMapper.class)
public interface EventFeedbackMapper {

    EventFeedbackSummary toSummary(EventFeedback feedback);
}
