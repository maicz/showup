package com.showup.api.service;

import com.showup.api.dto.AiDraftEventRequest;
import com.showup.api.dto.AiDraftEventResponse;
import com.showup.api.dto.AiFeedbackSummaryResponse;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventFeedback;
import com.showup.api.entity.Group;
import com.showup.api.entity.GroupTopic;
import com.showup.api.entity.MemberInterest;
import com.showup.api.entity.Topic;
import com.showup.api.enums.EventFormat;
import com.showup.api.mapper.EventMapper;
import com.showup.api.repository.EventFeedbackRepository;
import com.showup.api.repository.EventRepository;
import com.showup.api.repository.GroupTopicRepository;
import com.showup.api.repository.MemberInterestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CopilotServiceTest {

    @Mock
    private EventService eventService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventFeedbackRepository feedbackRepository;
    @Mock
    private MemberInterestRepository interestRepository;
    @Mock
    private GroupTopicRepository groupTopicRepository;
    @Mock
    private EventMapper eventMapper;

    private CopilotService service;

    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CopilotService(eventService, eventRepository, feedbackRepository, interestRepository, groupTopicRepository, eventMapper);
    }

    @Test
    void draftingOnlineEventDetectsFormatAndCategories() {
        AiDraftEventRequest request = new AiDraftEventRequest("Online workshop on Spring Boot and Java 25 microservices", null, null);

        AiDraftEventResponse response = service.draftEvent(request);

        assertThat(response.title()).contains("Online workshop on Spring Boot");
        assertThat(response.format()).isEqualTo(EventFormat.ONLINE);
        assertThat(response.suggestedCategorySlug()).isEqualTo("tech");
        assertThat(response.suggestedTopicSlugs()).contains("software-development");
    }

    @Test
    void summarizingFeedbackCalculatesSentimentAndAverage() {
        Event event = mock(Event.class);
        EventFeedback fb1 = mock(EventFeedback.class);
        EventFeedback fb2 = mock(EventFeedback.class);

        when(eventService.require(eventId)).thenReturn(event);
        when(fb1.getRating()).thenReturn(5);
        when(fb1.getComment()).thenReturn("Fantastic speaker and lively discussion!");
        when(fb2.getRating()).thenReturn(4);
        when(fb2.getComment()).thenReturn("Well structured, enjoyed it.");
        when(feedbackRepository.findAllByEventId(eventId)).thenReturn(List.of(fb1, fb2));

        AiFeedbackSummaryResponse summary = service.summarizeFeedback(eventId);

        assertThat(summary.averageRating()).isEqualTo(4.5);
        assertThat(summary.overallSentiment()).isEqualTo("Overwhelmingly Positive");
        assertThat(summary.positiveHighlights()).isNotEmpty();
    }

    @Test
    void recommendationsIncludeOnlyEventsMatchingFollowedTopics() {
        UUID memberId = UUID.randomUUID();
        UUID followedTopicId = UUID.randomUUID();
        UUID matchingGroupId = UUID.randomUUID();
        UUID otherGroupId = UUID.randomUUID();
        Event matchingEvent = mock(Event.class);
        Event otherEvent = mock(Event.class);
        Group matchingGroup = mock(Group.class);
        Group otherGroup = mock(Group.class);
        MemberInterest interest = mock(MemberInterest.class);
        Topic followedTopic = mock(Topic.class);
        GroupTopic matchingGroupTopic = mock(GroupTopic.class);

        when(interestRepository.findAllByMemberId(memberId)).thenReturn(List.of(interest));
        when(interest.getTopic()).thenReturn(followedTopic);
        when(followedTopic.getId()).thenReturn(followedTopicId);
        when(eventRepository.findAllByStatusOrderByStartsAtAsc(any())).thenReturn(List.of(matchingEvent, otherEvent));
        when(matchingEvent.getGroup()).thenReturn(matchingGroup);
        when(otherEvent.getGroup()).thenReturn(otherGroup);
        when(matchingGroup.getId()).thenReturn(matchingGroupId);
        when(otherGroup.getId()).thenReturn(otherGroupId);
        when(groupTopicRepository.findAllByGroupIdIn(argThat(ids -> ids.containsAll(List.of(matchingGroupId, otherGroupId))))).thenReturn(List.of(matchingGroupTopic));
        when(matchingGroupTopic.getTopic()).thenReturn(followedTopic);
        when(matchingGroupTopic.getGroup()).thenReturn(matchingGroup);

        service.getRecommendations(memberId);

        verify(eventMapper).toSummary(matchingEvent);
        verify(eventMapper, never()).toSummary(otherEvent);
    }
}
