package com.showup.api.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void totalPagesRoundsUpPartialPages() {
        PageResponse<String> page = PageResponse.of(List.of("a", "b"), 0, 20, 21);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void aZeroPageSizeReportsZeroTotalPagesInsteadOfDividingByZero() {
        PageResponse<String> page = PageResponse.of(List.of(), 0, 0, 0);
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void anExactMultipleDoesNotOverCountPages() {
        PageResponse<String> page = PageResponse.of(List.of(), 0, 10, 20);
        assertThat(page.totalPages()).isEqualTo(2);
    }
}
