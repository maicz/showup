package com.showup.api.mapper;

import com.showup.api.dto.PhotoSummary;
import com.showup.api.entity.EventPhoto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MemberMapper.class)
public interface EventPhotoMapper {

    PhotoSummary toSummary(EventPhoto photo);
}
