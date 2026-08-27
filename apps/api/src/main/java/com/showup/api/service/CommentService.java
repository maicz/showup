package com.showup.api.service;

import com.showup.api.dto.CommentSummary;
import com.showup.api.dto.CreateCommentRequest;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventComment;
import com.showup.api.entity.Member;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.mapper.EventCommentMapper;
import com.showup.api.repository.EventCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CommentService {

    private final EventCommentRepository comments;
    private final EventService eventService;
    private final MemberService memberService;
    private final GroupAccessGuard guard;
    private final EventCommentMapper mapper;

    CommentService(EventCommentRepository comments, EventService eventService,
                   MemberService memberService, GroupAccessGuard guard, EventCommentMapper mapper) {
        this.comments = comments;
        this.eventService = eventService;
        this.memberService = memberService;
        this.guard = guard;
        this.mapper = mapper;
    }

    public CommentSummary addComment(UUID actorId, UUID eventId, CreateCommentRequest request) {
        Event event = eventService.require(eventId);
        Member author = memberService.require(actorId);

        EventComment parent = null;
        if (request.parentCommentId() != null) {
            parent = comments.findById(request.parentCommentId())
                    .orElseThrow(() -> new NotFoundException("parent comment not found"));
            if (!parent.getEvent().getId().equals(eventId)) {
                throw new BusinessRuleException("parent comment belongs to a different event");
            }
            // Enforce single-level nesting per domain model
            if (parent.getParentComment() != null) {
                parent = parent.getParentComment();
            }
        }

        EventComment saved = comments.save(new EventComment(event, author, request.body().trim(), parent));
        return mapper.toSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentSummary> forEvent(UUID eventId) {
        eventService.require(eventId);
        return comments.findAllByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(comment -> {
                    CommentSummary summary = mapper.toSummary(comment);
                    if (comment.getDeletedAt() != null) {
                        return new CommentSummary(
                                summary.id(),
                                summary.author(),
                                "[comment deleted]",
                                summary.parentCommentId(),
                                summary.createdAt(),
                                summary.deletedAt());
                    }
                    return summary;
                })
                .toList();
    }

    public void deleteComment(UUID actorId, UUID eventId, UUID commentId) {
        Event event = eventService.require(eventId);
        EventComment comment = comments.findById(commentId)
                .orElseThrow(() -> new NotFoundException("comment not found"));

        if (!comment.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException("comment does not belong to this event");
        }

        boolean isAuthor = comment.getAuthor().getId().equals(actorId);
        boolean isOrganizer = guard.isEventAdmin(event.getGroup().getId(), actorId);

        if (!isAuthor && !isOrganizer) {
            throw new ForbiddenException("you cannot delete this comment");
        }

        comment.setDeletedAt(Instant.now());
    }
}
