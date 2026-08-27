package com.showup.api.controller;

import com.showup.api.dto.CommentSummary;
import com.showup.api.dto.MemberSummary;
import com.showup.api.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CommentService comments;

    @Test
    void listCommentsReturnsCommentsForEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        MemberSummary member = new MemberSummary(UUID.randomUUID(), "Alice", "alice.png");
        CommentSummary comment = new CommentSummary(commentId, member, "Looking forward to this!", null, Instant.now(), null);

        when(comments.forEvent(eventId)).thenReturn(List.of(comment));

        mvc.perform(get("/api/events/{eventId}/comments", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$[0].body").value("Looking forward to this!"))
                .andExpect(jsonPath("$[0].author.displayName").value("Alice"));
    }
}
