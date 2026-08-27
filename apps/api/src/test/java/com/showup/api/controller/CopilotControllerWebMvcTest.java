package com.showup.api.controller;

import com.showup.api.dto.AiDraftEventResponse;
import com.showup.api.enums.EventFormat;
import com.showup.api.service.CopilotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CopilotController.class)
@AutoConfigureMockMvc(addFilters = false)
class CopilotControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CopilotService copilot;

    @Test
    void draftEventGeneratesStructuredResponse() throws Exception {
        AiDraftEventResponse response = new AiDraftEventResponse(
                "Kubernetes Workshop", "Deep dive into Helm", EventFormat.IN_PERSON, "tech", List.of("devops"), 40, 90);
        when(copilot.draftEvent(any())).thenReturn(response);

        mvc.perform(post("/api/ai/copilot/draft-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Hands-on Kubernetes workshop with Helm charts\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Kubernetes Workshop"))
                .andExpect(jsonPath("$.format").value("IN_PERSON"))
                .andExpect(jsonPath("$.suggestedCategorySlug").value("tech"));
    }
}
