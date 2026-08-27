package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The member's own profile, interests, groups, and "my events" — all keyed off the bearer token. */
class MemberProfileFlowTest extends ApiIntegrationTest {

    @Test
    void aMemberCanUpdateAndReadTheirOwnProfile() throws Exception {
        String token = register("profile");

        mvc.perform(authed(put("/api/members/me"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"displayName":"New Name","bio":"Loves meetups","homeCity":"Cluj",
                                 "homeCountry":"RO"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"))
                .andExpect(jsonPath("$.homeCity").value("Cluj"));

        MvcResult me = mvc.perform(authed(get("/api/members/me"), token)).andReturn();
        UUID id = UUID.fromString(read(me).get("id").asString());

        mvc.perform(authed(get("/api/members/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"))
                .andExpect(jsonPath("$.bio").value("Loves meetups"))
                .andExpect(jsonPath("$.homeCity").value("Cluj"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.emailVerified").doesNotExist())
                .andExpect(jsonPath("$.homeCountry").doesNotExist());
    }

    @Test
    void interestsAreReplacedWholesaleNotMerged() throws Exception {
        String token = register("interests");
        JsonNode topics = read(mvc.perform(get("/api/topics")).andReturn());
        UUID first = UUID.fromString(topics.get(0).get("id").asString());
        UUID second = UUID.fromString(topics.get(1).get("id").asString());

        mvc.perform(authed(put("/api/members/me/interests"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicIds\":[\"" + first + "\",\"" + second + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Replacing with just one drops the other rather than adding to the set.
        mvc.perform(authed(put("/api/members/me/interests"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicIds\":[\"" + first + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(first.toString()));

        mvc.perform(authed(get("/api/members/me/interests"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updatingInterestsWithAnUnknownTopicIdIsRejected() throws Exception {
        String token = register("bad-interests");
        mvc.perform(authed(put("/api/members/me/interests"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void myGroupsAndMyEventsReflectActiveMembershipsAndSeatedRsvps() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());

        mvc.perform(authed(post("/api/groups/" + groupId + "/members"), attendee)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        mvc.perform(authed(get("/api/members/me/groups"), attendee))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(groupId.toString()));

        mvc.perform(authed(put("/api/events/" + eventId + "/rsvp"), attendee)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"YES","guestCount":0}"""))
                .andExpect(status().isOk());
        mvc.perform(authed(get("/api/members/me/events"), attendee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId.toString()))
                .andExpect(jsonPath("$[0].rsvpStatus").value("YES"))
                .andExpect(jsonPath("$[0].guestCount").value(0));

        // Withdrawing (RsvpStatus.NO) drops it from "my events".
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/events/" + eventId + "/rsvp").header("Authorization", "Bearer " + attendee))
                .andExpect(status().isOk());
        mvc.perform(authed(get("/api/members/me/events"), attendee))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- helpers ---

    private UUID createGroup(String token) throws Exception {
        String body = """
                {"urlname":"mp-%s","name":"Profile Group","categoryId":"%s","timeZone":"Europe/Bucharest",
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
                {"title":"Meetup","format":"IN_PERSON","venueId":"%s","startsAt":"%s",
                 "timeZone":"Europe/Bucharest","capacity":10,"waitlistEnabled":false,"guestsPerRsvpLimit":0,
                 "feeAmountMinor":0,"feeCurrency":"EUR"}"""
                .formatted(venueId, Instant.now().plus(30, ChronoUnit.DAYS));
        MvcResult result = mvc.perform(authed(post("/api/groups/" + groupId + "/events"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }
}
