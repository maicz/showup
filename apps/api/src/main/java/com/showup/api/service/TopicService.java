package com.showup.api.service;

import com.showup.api.dto.CategorySummary;
import com.showup.api.dto.TopicSummary;
import com.showup.api.mapper.TopicMapper;
import com.showup.api.repository.CategoryRepository;
import com.showup.api.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-only reference data: the browse taxonomy seeded by {@code R__seed_categories.sql}. */
@Service
@Transactional(readOnly = true)
public class TopicService {

    private final CategoryRepository categories;
    private final TopicRepository topics;
    private final TopicMapper mapper;

    TopicService(CategoryRepository categories, TopicRepository topics, TopicMapper mapper) {
        this.categories = categories;
        this.topics = topics;
        this.mapper = mapper;
    }

    public List<CategorySummary> categories() {
        return categories.findAllByOrderByDisplayOrderAsc().stream().map(mapper::toSummary).toList();
    }

    public List<TopicSummary> topics(String categorySlug) {
        if (categorySlug == null) {
            return topics.findAll().stream().map(mapper::toSummary).toList();
        }
        return categories.findBySlug(categorySlug)
                .map(category -> topics.findAllByCategoryId(category.getId()))
                .orElseGet(List::of)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }
}
