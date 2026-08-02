package com.showup.api.topic;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TopicMapper {

    CategorySummary toSummary(Category category);

    @Mapping(target = "categorySlug", source = "category.slug")
    TopicSummary toSummary(Topic topic);
}
