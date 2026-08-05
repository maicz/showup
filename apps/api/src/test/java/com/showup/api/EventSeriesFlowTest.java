package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recurring events: a series materializes real, individually-editable occurrence rows rather than
 * computing them on read. See docs/domain-model.md#eventseries.
 */
class EventSeriesFlowTest extends ApiIntegrationTest {

    @Test
    void createDetailAndMaterializeOccurrencesIdempotently() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);

        UUID seriesId = createSeries(organizer, groupId, venueId);

        mvc.perform(authed(get("/api/event-series/" + seriesId), organizer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateTitle").value("Weekly Standup"))
                .andExpect(jsonPath("$.templateVenue.id").value(venueId.toString()));

        mvc.perform(authed(get("/api/groups/" + groupId + "/event-series"), organizer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(seriesId.toString()));

        Instant first = Instant.parse("2027-01-04T18:00:00Z");
        MvcResult firstCall = mvc.perform(authed(post("/api/event-series/" + seriesId + "/occurrences"), organizer)
                        .param("firstStartsAt", first.toString())
                        .param("count", "3"))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(read(firstCall).size()).isEqualTo(3);

        // Calling materialize again with the same window must not duplicate the occurrences.
        mvc.perform(authed(post("/api/event-series/" + seriesId + "/occurrences"), organizer)
                        .param("firstStartsAt", first.toString())
                        .param("count", "3"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(authed(get("/api/event-series/" + seriesId + "/occurrences"), organizer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].format").value("IN_PERSON"));
    }

    @Test
    void aPlainMemberCannotCreateASeries() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        String outsider = register("outsider");

        String body = """
                {"groupId":"%s","recurrenceRule":"FREQ=WEEKLY;COUNT=5","templateTitle":"Standup",
                 "templateDurationMinutes":30}""".formatted(groupId);
        mvc.perform(authed(post("/api/event-series"), outsider)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void aMalformedRecurrenceRuleIsRejectedAtCreateNotAtMaterialize() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);

        String body = """
                {"groupId":"%s","recurrenceRule":"FREQ=FORTNIGHTLY","templateTitle":"Standup",
                 "templateDurationMinutes":30}""".formatted(groupId);
        mvc.perform(authed(post("/api/event-series"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aTemplateVenueFromAnotherGroupIsRejected() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID otherGroupId = createGroup(organizer);
        UUID otherVenueId = createVenue(organizer, otherGroupId);

        String body = """
                {"groupId":"%s","recurrenceRule":"FREQ=WEEKLY;COUNT=5","templateTitle":"Standup",
                 "templateVenueId":"%s","templateDurationMinutes":30}""".formatted(groupId, otherVenueId);
        mvc.perform(authed(post("/api/event-series"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- helpers ---

    private UUID createGroup(String token) throws Exception {
        String body = """
                {"urlname":"gs-%s","name":"Series Group","categoryId":"%s","timeZone":"Europe/Bucharest",
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

    private UUID createSeries(String token, UUID groupId, UUID venueId) throws Exception {
        String body = """
                {"groupId":"%s","recurrenceRule":"FREQ=WEEKLY;COUNT=12","templateTitle":"Weekly Standup",
                 "templateDescription":"Same time every week","templateVenueId":"%s",
                 "templateDurationMinutes":45}""".formatted(groupId, venueId);
        MvcResult result = mvc.perform(authed(post("/api/event-series"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode series = read(result);
        return UUID.fromString(series.get("id").asString());
    }
}
