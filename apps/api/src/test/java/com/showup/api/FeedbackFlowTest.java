package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feedback is only honest once the event actually happened, and only from people who attended —
 * see {@code FeedbackService.submit}. There is no "backdate an event" endpoint, so these tests
 * move a real event into the past through the ordinary update endpoint, the same way a slipped
 * schedule would in production.
 */
class FeedbackFlowTest extends ApiIntegrationTest {

    @Test
    void anAttendeeCanRateAnEventThatAlreadyHappenedExactlyOnce() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());
        rsvpYes(attendee, eventId);
        moveToThePast(organizer, eventId, venueId);

        mvc.perform(authed(post("/api/events/" + eventId + "/feedback"), attendee)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"rating":5,"comment":"Great talk"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));

        mvc.perform(authed(post("/api/events/" + eventId + "/feedback"), attendee)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"rating":3,"comment":"Second attempt"}"""))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/events/" + eventId + "/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].comment").value("Great talk"));

        // The group rating is a rollup recomputed from every rating its events received.
        mvc.perform(get("/api/groups/" + groupId))
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.ratingAverage").value(5.0));
    }

    @Test
    void someoneWhoNeverRsvpedYesCannotRateTheEvent() throws Exception {
        String organizer = register("organizer");
        String stranger = register("stranger");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());
        moveToThePast(organizer, eventId, venueId);

        mvc.perform(authed(post("/api/events/" + eventId + "/feedback"), stranger)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"rating":4,"comment":"n/a"}"""))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void feedbackIsRefusedWhileTheEventIsStillInTheFuture() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());
        rsvpYes(attendee, eventId);

        mvc.perform(authed(post("/api/events/" + eventId + "/feedback"), attendee)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"rating":5,"comment":"too soon"}"""))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- helpers ---

    private UUID createGroup(String token) throws Exception {
        String body = """
                {"urlname":"fb-%s","name":"Feedback Group","categoryId":"%s","timeZone":"Europe/Bucharest",
                 "visibility":"PUBLIC","joinPolicy":"OPEN"}"""
                .formatted(UUID.randomUUID().toString().substring(0, 8), firstCategoryId());
        MvcResult result = mvc.perform(authed(post("/api/groups"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }

    private UUID createVenue(String token, UUID groupId) throws Exception {
        String body = """
                {"name":"Impact Hub","addressLine1":"Str. Halelor 5","city":"Bucharest","country":"RO",
                 "location":{"latitude":44.4268,"longitude":26.1025}}""";
        MvcResult result = mvc.perform(authed(post("/api/groups/" + groupId + "/venues"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }

    private UUID createEvent(String token, UUID groupId, UUID venueId) throws Exception {
        String body = """
                {"title":"Retro","format":"IN_PERSON","venueId":"%s","startsAt":"%s",
                 "timeZone":"Europe/Bucharest","capacity":10,"waitlistEnabled":false,"guestsPerRsvpLimit":0,
                 "feeAmountMinor":0,"feeCurrency":"EUR"}"""
                .formatted(venueId, Instant.now().plus(30, ChronoUnit.DAYS));
        MvcResult result = mvc.perform(authed(post("/api/groups/" + groupId + "/events"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }

    private void rsvpYes(String token, UUID eventId) throws Exception {
        mvc.perform(authed(put("/api/events/" + eventId + "/rsvp"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"YES","guestCount":0}"""))
                .andExpect(status().isOk());
    }

    /** No "backdate" endpoint exists on purpose; an ordinary edit is how a schedule ever slips. */
    private void moveToThePast(String organizer, UUID eventId, UUID venueId) throws Exception {
        String body = """
                {"title":"Retro","format":"IN_PERSON","venueId":"%s","startsAt":"%s",
                 "timeZone":"Europe/Bucharest","capacity":10,"waitlistEnabled":false,"guestsPerRsvpLimit":0,
                 "feeAmountMinor":0,"feeCurrency":"EUR"}"""
                .formatted(venueId, Instant.now().minus(1, ChronoUnit.DAYS));
        mvc.perform(authed(put("/api/events/" + eventId), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }
}
