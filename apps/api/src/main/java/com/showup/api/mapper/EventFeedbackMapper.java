package com.showup.api.mapper;

import com.showup.api.dto.EventFeedbackSummary;
import com.showup.api.entity.EventFeedback;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MemberMapper.class)
public interface EventFeedbackMapper {

    EventFeedbackSummary toSummary(EventFeedback feedback);
}
