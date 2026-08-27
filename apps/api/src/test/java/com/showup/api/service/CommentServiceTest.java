package com.showup.api.service;

import com.showup.api.dto.CommentSummary;
import com.showup.api.dto.CreateCommentRequest;
import com.showup.api.dto.MemberSummary;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventComment;
import com.showup.api.entity.Group;
import com.showup.api.entity.Member;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.mapper.EventCommentMapper;
import com.showup.api.repository.EventCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private EventCommentRepository comments;
    @Mock
    private EventService eventService;
    @Mock
    private MemberService memberService;
    @Mock
    private GroupAccessGuard guard;
    @Mock
    private EventCommentMapper mapper;

    private CommentService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID commentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CommentService(comments, eventService, memberService, guard, mapper);
    }

    @Test
    void addingCommentPersistsAndMaps() {
        Event event = mock(Event.class);
        Member author = mock(Member.class);
        EventComment savedComment = mock(EventComment.class);
        MemberSummary memberSummary = new MemberSummary(actorId, "Alice", "alice.jpg");
        CommentSummary expected = new CommentSummary(commentId, memberSummary, "Great meetup!", null, Instant.now(), null);

        when(eventService.require(eventId)).thenReturn(event);
        when(memberService.require(actorId)).thenReturn(author);
        when(comments.save(any(EventComment.class))).thenReturn(savedComment);
        when(mapper.toSummary(savedComment)).thenReturn(expected);

        CommentSummary result = service.addComment(actorId, eventId, new CreateCommentRequest("Great meetup!", null));

        assertThat(result).isEqualTo(expected);
        verify(comments).save(any(EventComment.class));
    }

    @Test
    void deletingCommentByAuthorMarksDeletedAt() {
        Event event = mock(Event.class);
        Group group = mock(Group.class);
        Member author = mock(Member.class);
        EventComment comment = new EventComment(event, author, "some text", null);

        when(eventService.require(eventId)).thenReturn(event);
        when(event.getId()).thenReturn(eventId);
        when(event.getGroup()).thenReturn(group);
        when(group.getId()).thenReturn(UUID.randomUUID());
        when(author.getId()).thenReturn(actorId);
        when(comments.findById(commentId)).thenReturn(Optional.of(comment));
        when(guard.isEventAdmin(any(), any())).thenReturn(false);

        service.deleteComment(actorId, eventId, commentId);

        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    void deletingCommentByUnauthorizedMemberThrowsForbidden() {
        Event event = mock(Event.class);
        Group group = mock(Group.class);
        Member author = mock(Member.class);
        EventComment comment = new EventComment(event, author, "some text", null);

        when(eventService.require(eventId)).thenReturn(event);
        when(event.getId()).thenReturn(eventId);
        when(event.getGroup()).thenReturn(group);
        when(group.getId()).thenReturn(UUID.randomUUID());
        when(author.getId()).thenReturn(UUID.randomUUID()); // different member
        when(comments.findById(commentId)).thenReturn(Optional.of(comment));
        when(guard.isEventAdmin(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.deleteComment(actorId, eventId, commentId))
                .isInstanceOf(ForbiddenException.class);
    }
}
