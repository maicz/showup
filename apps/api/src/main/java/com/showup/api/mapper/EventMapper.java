package com.showup.api.mapper;

import com.showup.api.dto.EventDetail;
import com.showup.api.dto.EventSummary;
import com.showup.api.dto.MemberSummary;
import com.showup.api.dto.RsvpSummary;
import com.showup.api.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {GroupMapper.class, VenueMapper.class})
public interface EventMapper {

    @Mapping(target = "venueCity", expression = "java(event.getVenue() != null ? event.getVenue().getAddress().getCity() : null)")
    @Mapping(target = "availability", expression = "java(event.getAvailability())")
    EventSummary toSummary(Event event);

    @Mapping(target = "availability", expression = "java(event.getAvailability())")
    @Mapping(target = "hosts", ignore = true)
    @Mapping(target = "viewerRsvp", ignore = true)
    EventDetail toDetailWithoutContext(Event event);

    /** {@code hosts} and {@code viewerRsvp} need extra queries the entity alone can't provide. */
    default EventDetail toDetail(Event event, List<MemberSummary> hosts, RsvpSummary viewerRsvp) {
        EventDetail base = toDetailWithoutContext(event);
        return new EventDetail(
                base.id(), base.title(), base.description(), base.status(),
                base.format(), base.venue(), base.onlineUrl(),
                base.startsAt(), base.endsAt(), base.timeZone(),
                base.capacity(), base.waitlistEnabled(), base.guestsPerRsvpLimit(),
                base.fee(), base.rsvpOpensAt(), base.rsvpClosesAt(),
                base.yesRsvpCount(), base.waitlistCount(), base.availability(),
                base.group(), hosts, viewerRsvp);
    }
}
