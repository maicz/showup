package com.showup.api.service;

import com.showup.api.dto.AiDraftEventRequest;
import com.showup.api.dto.AiDraftEventResponse;
import com.showup.api.dto.AiFeedbackSummaryResponse;
import com.showup.api.dto.AiRecommendationsResponse;
import com.showup.api.dto.EventSummary;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventFeedback;
import com.showup.api.entity.MemberInterest;
import com.showup.api.enums.EventFormat;
import com.showup.api.enums.EventStatus;
import com.showup.api.mapper.EventMapper;
import com.showup.api.repository.EventFeedbackRepository;
import com.showup.api.repository.EventRepository;
import com.showup.api.repository.GroupTopicRepository;
import com.showup.api.repository.MemberInterestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CopilotService {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final EventFeedbackRepository feedbackRepository;
    private final MemberInterestRepository interestRepository;
    private final GroupTopicRepository groupTopicRepository;
    private final EventMapper eventMapper;

    CopilotService(EventService eventService,
                   EventRepository eventRepository,
                   EventFeedbackRepository feedbackRepository,
                   MemberInterestRepository interestRepository,
                   GroupTopicRepository groupTopicRepository,
                   EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.feedbackRepository = feedbackRepository;
        this.interestRepository = interestRepository;
        this.groupTopicRepository = groupTopicRepository;
        this.eventMapper = eventMapper;
    }

    public AiDraftEventResponse draftEvent(AiDraftEventRequest request) {
        String prompt = request.prompt().trim();
        String lowerPrompt = prompt.toLowerCase(Locale.ROOT);

        EventFormat format = EventFormat.IN_PERSON;
        if (lowerPrompt.contains("online") || lowerPrompt.contains("zoom") || lowerPrompt.contains("virtual")) {
            format = EventFormat.ONLINE;
        } else if (lowerPrompt.contains("hybrid")) {
            format = EventFormat.HYBRID;
        }

        String title = generateTitleFromPrompt(prompt);
        String categorySlug = "tech";
        List<String> topicSlugs = new ArrayList<>(List.of("software-development", "technology"));

        if (lowerPrompt.contains("book") || lowerPrompt.contains("reading") || lowerPrompt.contains("club")) {
            categorySlug = "hobbies-crafts";
            topicSlugs = List.of("reading", "literature", "creative-writing");
        } else if (lowerPrompt.contains("hiking") || lowerPrompt.contains("run") || lowerPrompt.contains("fitness") || lowerPrompt.contains("yoga")) {
            categorySlug = "sports-fitness";
            topicSlugs = List.of("fitness", "wellness", "running");
        } else if (lowerPrompt.contains("game") || lowerPrompt.contains("board") || lowerPrompt.contains("dnd")) {
            categorySlug = "games-social";
            topicSlugs = List.of("board-games", "card-games", "roleplaying");
        } else if (lowerPrompt.contains("art") || lowerPrompt.contains("paint") || lowerPrompt.contains("draw")) {
            categorySlug = "art-culture";
            topicSlugs = List.of("painting", "art-workshops", "visual-arts");
        }

        String structuredDescription = "## About This Event\n\n"
                + prompt + "\n\n"
                + "### What to Expect\n"
                + "- Welcome and introductions\n"
                + "- Interactive discussion & activities\n"
                + "- Open Q&A and networking\n\n"
                + "### What to Bring\n"
                + "- Your curiosity and enthusiasm\n"
                + "- Any required materials or questions";

        return new AiDraftEventResponse(
                title,
                structuredDescription,
                format,
                categorySlug,
                topicSlugs,
                30,
                120);
    }

    public AiFeedbackSummaryResponse summarizeFeedback(UUID eventId) {
        eventService.require(eventId);
        List<EventFeedback> feedbackList = feedbackRepository.findAllByEventId(eventId);

        if (feedbackList.isEmpty()) {
            return new AiFeedbackSummaryResponse(
                    "Neutral",
                    0.0,
                    List.of("No feedback recorded yet"),
                    List.of(),
                    List.of("Encourage attendees to submit reviews after the session"),
                    "No attendee ratings or comments have been submitted for this event yet.");
        }

        double avg = feedbackList.stream().mapToInt(EventFeedback::getRating).average().orElse(0.0);
        String sentiment = avg >= 4.5 ? "Overwhelmingly Positive"
                : avg >= 3.8 ? "Positive"
                : avg >= 2.5 ? "Mixed" : "Needs Improvement";

        List<String> positive = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> themes = new ArrayList<>();

        for (EventFeedback fb : feedbackList) {
            String comment = fb.getComment();
            if (comment != null && !comment.isBlank()) {
                if (fb.getRating() >= 4) {
                    positive.add(comment);
                } else {
                    suggestions.add(comment);
                }
            }
        }

        if (positive.isEmpty()) {
            positive.add("Attendees appreciated the overall organization and host clarity.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Keep up the pacing and consider follow-up breakout rooms.");
        }

        themes.add("Pacing and Content Depth");
        themes.add("Attendee Engagement & Q&A");
        themes.add("Venue Logistics & Amenities");

        String narrative = String.format(
                "Feedback for this event is %s with an average score of %.1f/5.0 across %d attendee reviews. "
                        + "Key strengths highlighted were presentation quality and community engagement.",
                sentiment.toLowerCase(Locale.ROOT), avg, feedbackList.size());

        return new AiFeedbackSummaryResponse(
                sentiment,
                Math.round(avg * 10.0) / 10.0,
                themes,
                positive.stream().limit(3).toList(),
                suggestions.stream().limit(3).toList(),
                narrative);
    }

    public AiRecommendationsResponse getRecommendations(UUID memberId) {
        List<MemberInterest> interests = interestRepository.findAllByMemberId(memberId);
        Set<UUID> interestedTopicIds = interests.stream()
                .map(i -> i.getTopic().getId())
                .collect(Collectors.toSet());

        List<Event> upcoming = eventRepository.findAllByStatusOrderByStartsAtAsc(EventStatus.PUBLISHED);

        Set<UUID> upcomingGroupIds = upcoming.stream()
                .map(event -> event.getGroup().getId())
                .collect(Collectors.toSet());
        Set<UUID> matchingGroupIds = interestedTopicIds.isEmpty() || upcomingGroupIds.isEmpty()
                ? Set.of()
                : groupTopicRepository.findAllByGroupIdIn(List.copyOf(upcomingGroupIds))
                        .stream()
                        .filter(groupTopic -> interestedTopicIds.contains(groupTopic.getTopic().getId()))
                        .map(groupTopic -> groupTopic.getGroup().getId())
                        .collect(Collectors.toSet());

        List<Event> ranked = interestedTopicIds.isEmpty()
                ? upcoming
                : upcoming.stream()
                        .filter(event -> matchingGroupIds.contains(event.getGroup().getId()))
                        .toList();

        List<EventSummary> recommended = ranked.stream()
                .limit(6)
                .map(eventMapper::toSummary)
                .toList();

        String rationale = interestedTopicIds.isEmpty()
                ? "Popular upcoming events across active community groups near you."
                : "Curated based on your followed topics and interests.";

        return new AiRecommendationsResponse(recommended, rationale);
    }

    private String generateTitleFromPrompt(String prompt) {
        String cleaned = prompt.lines().findFirst().orElse(prompt).trim();
        if (cleaned.length() > 60) {
            cleaned = cleaned.substring(0, 57) + "...";
        }
        return cleaned;
    }
}
