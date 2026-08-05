package com.showup.api.controller;

import com.showup.api.dto.CategorySummary;
import com.showup.api.dto.TopicSummary;
import com.showup.api.service.TopicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A {@code @WebMvcTest} slice: Spring starts only {@link TopicController} plus the ordinary MVC
 * infrastructure (argument resolvers, converters, the exception advice) — no service beans beyond
 * the mocked one, no repositories, no security filter chain, no database, no Testcontainers. That
 * is the whole point of the slice versus the {@code @SpringBootTest} classes elsewhere in this
 * suite: this test class boots in well under a second because the context it builds is tiny.
 *
 * <p>Security is switched off with {@code addFilters = false} rather than imported, since these
 * two endpoints are anonymous in the real filter chain anyway and the point here is the
 * controller's request/response mapping, not authorization — {@link GroupControllerSecurityWebMvcTest}
 * covers the slice-plus-security combination.
 */
@WebMvcTest(TopicController.class)
@AutoConfigureMockMvc(addFilters = false)
class TopicControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TopicService topics;

    @Test
    void categoriesAreReturnedInWhateverOrderTheServiceProvidesThem() throws Exception {
        UUID id = UUID.randomUUID();
        when(topics.categories()).thenReturn(List.of(new CategorySummary(id, "tech", "Technology", null, 1)));

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("tech"))
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    void topicsForwardsTheOptionalCategorySlugQueryParameter() throws Exception {
        when(topics.topics("tech")).thenReturn(
                List.of(new TopicSummary(UUID.randomUUID(), "backend", "Backend", "tech", 0)));

        mvc.perform(get("/api/topics").param("categorySlug", "tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("backend"));
        verify(topics).topics("tech");
    }

    @Test
    void anAbsentCategorySlugIsPassedThroughAsNull() throws Exception {
        when(topics.topics(null)).thenReturn(List.of());

        mvc.perform(get("/api/topics")).andExpect(status().isOk());
        verify(topics).topics(null);
    }
}
