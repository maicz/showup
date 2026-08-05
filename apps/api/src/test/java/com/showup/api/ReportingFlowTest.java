package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Organizer-only rollups over a date window — see {@code ReportingService.groupActivity}. */
class ReportingFlowTest extends ApiIntegrationTest {

    @Test
    void groupActivityRollsUpEveryHostedEventInTheWindow() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());
        mvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/events/" + eventId + "/rsvp"), attendee)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"YES","guestCount":1}"""))
                .andExpect(status().isOk());

        String from = Instant.now().minus(1, ChronoUnit.DAYS).toString();
        String to = Instant.now().plus(60, ChronoUnit.DAYS).toString();
        mvc.perform(authed(get("/api/groups/" + groupId + "/reports/activity"), organizer)
                        .param("from", from).param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventsHosted").value(1))
                .andExpect(jsonPath("$.totalRsvps").value(1))
                .andExpect(jsonPath("$.groupName").exists());
    }

    @Test
    void reportsAreOrganizerOnly() throws Exception {
        String organizer = register("organizer");
        String outsider = register("outsider");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);

        String from = Instant.now().minus(1, ChronoUnit.DAYS).toString();
        String to = Instant.now().plus(60, ChronoUnit.DAYS).toString();
        mvc.perform(authed(get("/api/groups/" + groupId + "/reports/activity"), outsider)
                        .param("from", from).param("to", to))
                .andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/events/" + eventId + "/reports/attendance"), outsider))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private UUID createGroup(String token) throws Exception {
        String body = """
                {"urlname":"rp-%s","name":"Reporting Group","categoryId":"%s","timeZone":"Europe/Bucharest",
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
                {"title":"Report Test","format":"IN_PERSON","venueId":"%s","startsAt":"%s",
                 "timeZone":"Europe/Bucharest","capacity":10,"waitlistEnabled":false,"guestsPerRsvpLimit":2,
                 "feeAmountMinor":0,"feeCurrency":"EUR"}"""
                .formatted(venueId, Instant.now().plus(30, ChronoUnit.DAYS));
        MvcResult result = mvc.perform(authed(post("/api/groups/" + groupId + "/events"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }
}
