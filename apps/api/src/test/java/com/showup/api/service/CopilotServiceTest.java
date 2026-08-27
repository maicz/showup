package com.showup.api.service;

import com.showup.api.dto.AiDraftEventRequest;
import com.showup.api.dto.AiDraftEventResponse;
import com.showup.api.dto.AiFeedbackSummaryResponse;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventFeedback;
import com.showup.api.enums.EventFormat;
import com.showup.api.mapper.EventMapper;
import com.showup.api.repository.EventFeedbackRepository;
import com.showup.api.repository.EventRepository;
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
    private EventMapper eventMapper;

    private CopilotService service;

    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CopilotService(eventService, eventRepository, feedbackRepository, interestRepository, eventMapper);
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
}
