package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Small, otherwise-homeless behaviors: the anonymous browse taxonomy, single-venue lookup, the
 * event detail endpoint's anonymous-vs-signed-in branch, and the malformed-input corners of
 * {@code ApiExceptionHandler} that a well-formed request never reaches.
 */
class MiscApiFlowTest extends ApiIntegrationTest {

    @Test
    void topicsCanBeBrowsedAsAWholeOrFilteredByCategory() throws Exception {
        MvcResult categories = mvc.perform(get("/api/categories")).andReturn();
        JsonNode categoryList = read(categories);
        assertThat(categoryList.size()).isGreaterThan(1);
        // Ordered by displayOrder ascending, whatever the seeded values happen to be.
        int previousOrder = Integer.MIN_VALUE;
        for (JsonNode node : categoryList) {
            int order = node.get("displayOrder").asInt();
            assertThat(order).isGreaterThanOrEqualTo(previousOrder);
            previousOrder = order;
        }
        MvcResult allTopics = mvc.perform(get("/api/topics")).andReturn();
        JsonNode topicList = read(allTopics);
        assertThat(topicList.size()).isGreaterThan(1);
        // Pick a category slug known to actually have a topic under it, rather than assuming
        // anything about which category sorts first.
        String categorySlug = topicList.get(0).get("categorySlug").asString();

        mvc.perform(get("/api/topics").param("categorySlug", categorySlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categorySlug").value(categorySlug));

        mvc.perform(get("/api/topics").param("categorySlug", "no-such-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aSingleVenueIsReadableByIdAndAnUnknownOneIs404() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);

        mvc.perform(authed(get("/api/venues/" + venueId), organizer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Impact Hub"));

        mvc.perform(authed(get("/api/venues/" + UUID.randomUUID()), organizer))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventDetailIsPublicButRichierWhenSignedIn() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());

        // No bearer token at all: viewerRsvp resolves through the Optional<UUID> branch as absent.
        mvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerRsvp").doesNotExist());
    }

    @Test
    void aMalformedPathVariableAndBodyBothReportAsBadRequest() throws Exception {
        mvc.perform(get("/api/events/not-a-uuid"))
                .andExpect(status().isBadRequest());

        String organizer = register("organizer");
        mvc.perform(authed(post("/api/groups"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content("{not valid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aGlobalCrossFieldValidationErrorIsReportedByObjectNameNotByField() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);

        // format IN_PERSON with a venue AND endsAt before startsAt trips the class-level
        // @ValidEventRequest constraint, which has no single field to attach to.
        Instant starts = Instant.now().plus(30, ChronoUnit.DAYS);
        String body = """
                {"title":"Backwards","format":"IN_PERSON","venueId":"%s","startsAt":"%s","endsAt":"%s",
                 "timeZone":"Europe/Bucharest","waitlistEnabled":false,"guestsPerRsvpLimit":0,
                 "feeAmountMinor":0,"feeCurrency":"EUR"}""".formatted(venueId, starts, starts.minusSeconds(60));
        mvc.perform(authed(post("/api/groups/" + groupId + "/events"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("createEventRequest"));
    }

    // --- helpers ---

    private UUID createGroup(String token) throws Exception {
        String body = """
                {"urlname":"mi-%s","name":"Misc Group","categoryId":"%s","timeZone":"Europe/Bucharest",
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
                {"title":"Misc Event","format":"IN_PERSON","venueId":"%s","startsAt":"%s",
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
