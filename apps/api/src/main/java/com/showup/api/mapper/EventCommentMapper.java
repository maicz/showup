package com.showup.api.mapper;

import com.showup.api.dto.CommentSummary;
import com.showup.api.entity.EventComment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MemberMapper.class)
public interface EventCommentMapper {

    @Mapping(target = "parentCommentId", source = "parentComment.id")
    CommentSummary toSummary(EventComment comment);
}
