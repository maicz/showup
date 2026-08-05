package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Public-facing event hosts, distinct from operational staff — see {@code EventService}. */
class EventHostFlowTest extends ApiIntegrationTest {

    @Test
    void hostsCanBeAddedAndRemovedButOneMustAlwaysRemain() throws Exception {
        String organizer = register("organizer");
        String cohost = register("cohost");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        UUID cohostId = memberId(cohost);
        join(cohost, groupId);

        mvc.perform(authed(post("/api/events/" + eventId + "/hosts"), organizer)
                        .param("memberId", cohostId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));

        // Adding the same host twice is a conflict.
        mvc.perform(authed(post("/api/events/" + eventId + "/hosts"), organizer)
                        .param("memberId", cohostId.toString()))
                .andExpect(status().isConflict());

        UUID organizerId = memberId(organizer);
        mvc.perform(authed(delete("/api/events/" + eventId + "/hosts/" + organizerId), organizer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Now only the co-host remains; removing the last host is refused.
        mvc.perform(authed(delete("/api/events/" + eventId + "/hosts/" + cohostId), organizer))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aHostMustBeAnActiveMemberOfTheGroup() throws Exception {
        String organizer = register("organizer");
        String outsider = register("outsider");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        UUID outsiderId = memberId(outsider);

        mvc.perform(authed(post("/api/events/" + eventId + "/hosts"), organizer)
                        .param("memberId", outsiderId.toString()))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private UUID createGroup(String token) throws Exception {
        String body = """
                {"urlname":"eh-%s","name":"Host Group","categoryId":"%s","timeZone":"Europe/Bucharest",
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
                {"title":"Hosted Talk","format":"IN_PERSON","venueId":"%s","startsAt":"%s",
                 "timeZone":"Europe/Bucharest","capacity":10,"waitlistEnabled":false,"guestsPerRsvpLimit":0,
                 "feeAmountMinor":0,"feeCurrency":"EUR"}"""
                .formatted(venueId, Instant.now().plus(30, ChronoUnit.DAYS));
        MvcResult result = mvc.perform(authed(post("/api/groups/" + groupId + "/events"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }

    private UUID memberId(String token) throws Exception {
        MvcResult result = mvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/members/me"), token))
                .andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }

    private void join(String token, UUID groupId) throws Exception {
        mvc.perform(authed(post("/api/groups/" + groupId + "/members"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
    }
}
