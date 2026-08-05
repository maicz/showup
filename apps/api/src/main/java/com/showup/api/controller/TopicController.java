package com.showup.api.controller;

import com.showup.api.dto.CategorySummary;
import com.showup.api.dto.TopicSummary;
import com.showup.api.service.TopicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The browse taxonomy. Anonymous — it is the first thing a signed-out visitor sees. */
@RestController
@RequestMapping("/api")
public class TopicController {

    private final TopicService topics;

    TopicController(TopicService topics) {
        this.topics = topics;
    }

    @GetMapping("/categories")
    public List<CategorySummary> categories() {
        return topics.categories();
    }

    @GetMapping("/topics")
    public List<TopicSummary> topics(@RequestParam(required = false) String categorySlug) {
        return topics.topics(categorySlug);
    }
}
