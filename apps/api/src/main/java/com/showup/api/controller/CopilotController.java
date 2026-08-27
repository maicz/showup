package com.showup.api.controller;

import com.showup.api.dto.AiDraftEventRequest;
import com.showup.api.dto.AiDraftEventResponse;
import com.showup.api.dto.AiFeedbackSummaryResponse;
import com.showup.api.dto.AiRecommendationsResponse;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.CopilotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class CopilotController {

    private final CopilotService copilot;

    CopilotController(CopilotService copilot) {
        this.copilot = copilot;
    }

    @PostMapping("/copilot/draft-event")
    public AiDraftEventResponse draftEvent(@Valid @RequestBody AiDraftEventRequest request) {
        return copilot.draftEvent(request);
    }

    @GetMapping("/feedback/{eventId}/summary")
    public AiFeedbackSummaryResponse summarizeFeedback(@PathVariable UUID eventId) {
        return copilot.summarizeFeedback(eventId);
    }

    @GetMapping("/recommendations")
    public AiRecommendationsResponse recommendations(@CurrentMember UUID actor) {
        return copilot.getRecommendations(actor);
    }
}
