package com.showup.api.service;

import com.showup.api.dto.EventFeedbackSummary;
import com.showup.api.dto.SubmitFeedbackRequest;
import com.showup.api.entity.Event;
import com.showup.api.entity.EventFeedback;
import com.showup.api.entity.Group;
import com.showup.api.entity.Rsvp;
import com.showup.api.enums.RsvpStatus;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ConflictException;
import com.showup.api.mapper.EventFeedbackMapper;
import com.showup.api.repository.EventFeedbackRepository;
import com.showup.api.repository.RsvpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FeedbackService {

    private final EventFeedbackRepository feedback;
    private final RsvpRepository rsvps;
    private final EventService eventService;
    private final MemberService memberService;
    private final EventFeedbackMapper mapper;

    FeedbackService(EventFeedbackRepository feedback, RsvpRepository rsvps, EventService eventService,
                    MemberService memberService, EventFeedbackMapper mapper) {
        this.feedback = feedback;
        this.rsvps = rsvps;
        this.eventService = eventService;
        this.memberService = memberService;
        this.mapper = mapper;
    }

    public EventFeedbackSummary submit(UUID actorId, UUID eventId, SubmitFeedbackRequest request) {
        Event event = eventService.require(eventId);
        if (event.getStartsAt().isAfter(Instant.now())) {
            throw new BusinessRuleException("the event has not happened yet");
        }
        // Ratings from people who never signed up would make the group rating meaningless.
        Rsvp rsvp = rsvps.findByEventIdAndMemberId(eventId, actorId)
                .filter(r -> r.getStatus() == RsvpStatus.YES)
                .orElseThrow(() -> new BusinessRuleException("only attendees can rate this event"));
        feedback.findByEventIdAndMemberId(eventId, actorId).ifPresent(existing -> {
            throw new ConflictException("you have already rated this event");
        });

        EventFeedback saved = feedback.save(new EventFeedback(
                rsvp.getEvent(), memberService.require(actorId), request.rating(), request.comment()));
        recomputeGroupRating(event.getGroup());
        return mapper.toSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<EventFeedbackSummary> forEvent(UUID eventId) {
        return feedback.findAllByEventId(eventId).stream().map(mapper::toSummary).toList();
    }

    /**
     * The group rating is a denormalized rollup of every rating its events received. Recomputed
     * from scratch rather than nudged incrementally, so it cannot drift away from the rows.
     */
    private void recomputeGroupRating(Group group) {
        feedback.flush();
        Double average = feedback.averageRatingForGroup(group.getId());
        long count = feedback.countForGroup(group.getId());
        group.setRatingAverage(average == null ? null
                : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP));
        group.setRatingCount((int) count);
    }
}
