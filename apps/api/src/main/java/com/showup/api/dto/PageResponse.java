package com.showup.api.dto;

import java.util.List;

/**
 * Wraps list results instead of returning Spring's {@code Page} — {@code Page} serializes an
 * unstable internal shape that Spring Boot warns about, and pins the API to Spring's class
 * layout. See docs/domain-model.md#dtos.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
