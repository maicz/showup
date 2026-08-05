package com.showup.api.controller;

import com.showup.api.dto.EventFeedbackSummary;
import com.showup.api.dto.SubmitFeedbackRequest;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/feedback")
public class FeedbackController {

    private final FeedbackService feedback;

    FeedbackController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFeedbackSummary submit(@CurrentMember UUID actor, @PathVariable UUID eventId,
                                       @Valid @RequestBody SubmitFeedbackRequest request) {
        return feedback.submit(actor, eventId, request);
    }

    @GetMapping
    public List<EventFeedbackSummary> list(@PathVariable UUID eventId) {
        return feedback.forEvent(eventId);
    }
}
