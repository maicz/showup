package com.showup.api.mapper;

import com.showup.api.dto.CategorySummary;
import com.showup.api.dto.TopicSummary;
import com.showup.api.entity.Category;
import com.showup.api.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TopicMapper {

    CategorySummary toSummary(Category category);

    @Mapping(target = "categorySlug", source = "category.slug")
    TopicSummary toSummary(Topic topic);
}
