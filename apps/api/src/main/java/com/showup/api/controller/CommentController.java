package com.showup.api.controller;

import com.showup.api.dto.CommentSummary;
import com.showup.api.dto.CreateCommentRequest;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/events/{eventId}/comments")
public class CommentController {

    private final CommentService comments;

    CommentController(CommentService comments) {
        this.comments = comments;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentSummary addComment(@CurrentMember UUID actor, @PathVariable UUID eventId,
                                     @Valid @RequestBody CreateCommentRequest request) {
        return comments.addComment(actor, eventId, request);
    }

    @GetMapping
    public List<CommentSummary> list(@PathVariable UUID eventId) {
        return comments.forEvent(eventId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@CurrentMember UUID actor, @PathVariable UUID eventId,
                              @PathVariable UUID commentId) {
        comments.deleteComment(actor, eventId, commentId);
    }
}
