package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole organizer workflow the README names as the next step: create the event, publish it,
 * take RSVPs past capacity, promote off the waitlist, issue a ticket, scan it, and report on it.
 */
class OrganizerFlowTest extends ApiIntegrationTest {

    @Test
    void createPublishRsvpWaitlistTicketCheckInAndReport() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        // Capacity of one, waitlist on: the smallest event that exercises seat allocation.
        UUID eventId = createEvent(organizer, groupId, venueId, 1, true);

        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.availability").value("SEATS_AVAILABLE"));

        String first = register("first");
        String second = register("second");

        rsvpYes(first, eventId).andExpect(jsonPath("$.status").value("YES"));
        // The event is now full, so the next YES becomes a waitlist placement, not a refusal.
        rsvpYes(second, eventId)
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1));

        mvc.perform(authed(get("/api/events/" + eventId), second))
                .andExpect(jsonPath("$.availability").value("WAITLIST"))
                .andExpect(jsonPath("$.viewerRsvp.status").value("WAITLISTED"));

        // First withdraws; the seat must go to the head of the waitlist automatically.
        mvc.perform(authed(delete("/api/events/" + eventId + "/rsvp"), first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO"));
        mvc.perform(authed(get("/api/events/" + eventId + "/rsvp"), second))
                .andExpect(jsonPath("$.status").value("YES"))
                .andExpect(jsonPath("$.waitlistPosition").doesNotExist());

        MvcResult ticket = mvc.perform(authed(post("/api/events/" + eventId + "/ticket"), second))
                .andExpect(status().isOk())
                .andReturn();
        String code = read(ticket).get("code").asString();

        String checkIn = """
                {"ticketCode":"%s","method":"QR_SCAN","admittedCount":1}""".formatted(code);
        MvcResult firstScan = mvc.perform(authed(post("/api/events/" + eventId + "/check-ins"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(checkIn))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult secondScan = mvc.perform(authed(post("/api/events/" + eventId + "/check-ins"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(checkIn))
                .andExpect(status().isOk())
                .andReturn();
        // Scanning twice is a fact of life at a door; it must not admit two people.
        assertThat(read(secondScan).get("id").asString()).isEqualTo(read(firstScan).get("id").asString());

        mvc.perform(authed(get("/api/events/" + eventId + "/reports/attendance"), organizer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredCount").value(1))
                .andExpect(jsonPath("$.attendedCount").value(1))
                .andExpect(jsonPath("$.noShowCount").value(0))
                .andExpect(jsonPath("$.attendanceRate").value(1.0))
                .andExpect(jsonPath("$.staffScanTotals.length()").value(1));
    }

    @Test
    void aPlainMemberCannotPublishSomeoneElsesEvent() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID eventId = createEvent(organizer, groupId, createVenue(organizer, groupId), 10, false);

        String outsider = register("outsider");
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), outsider))
                .andExpect(status().isForbidden());

        // Joining the group is not enough either — publishing needs an organizer role.
        mvc.perform(authed(post("/api/groups/" + groupId + "/members"), outsider)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"introduction":"hello"}"""))
                .andExpect(status().isCreated());
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), outsider))
                .andExpect(status().isForbidden());
    }

    @Test
    void anEventCannotGoStraightFromCancelledBackToPublished() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID eventId = createEvent(organizer, groupId, createVenue(organizer, groupId), 10, false);

        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer))
                .andExpect(status().isOk());
        mvc.perform(authed(post("/api/events/" + eventId + "/cancel"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"reason":"the venue flooded"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer))
                .andExpect(status().isUnprocessableEntity());
        // And a cancelled event stops taking RSVPs.
        mvc.perform(authed(put("/api/events/" + eventId + "/rsvp"), register("late"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"YES","guestCount":0}"""))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aFullEventWithNoWaitlistRefusesTheRsvp() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID eventId = createEvent(organizer, groupId, createVenue(organizer, groupId), 1, false);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());

        rsvpYes(register("only"), eventId).andExpect(jsonPath("$.status").value("YES"));
        mvc.perform(authed(put("/api/events/" + eventId + "/rsvp"), register("turned-away"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"YES","guestCount":0}"""))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void theCrossFieldEventConstraintRejectsAnOnlineEventWithNoUrl() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);

        String body = """
                {"title":"Nowhere","format":"ONLINE","startsAt":"%s","timeZone":"Europe/Bucharest",
                 "waitlistEnabled":false,"guestsPerRsvpLimit":0,"feeAmountMinor":0,"feeCurrency":"EUR"}"""
                .formatted(Instant.now().plus(30, ChronoUnit.DAYS));

        mvc.perform(authed(post("/api/groups/" + groupId + "/events"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchFiltersOnTheFacetsTheProductExposes() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer);
        UUID eventId = createEvent(organizer, groupId, createVenue(organizer, groupId), 10, false);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());

        mvc.perform(get("/api/events/search").param("format", "IN_PERSON").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + eventId + "')]").exists());
        // Same event, filtered out by price rather than by anything about the event itself.
        mvc.perform(get("/api/events/search").param("maxFee", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + eventId + "')]").doesNotExist());
    }

    // --- helpers ---

    private UUID createGroup(String token) throws Exception {
        String body = """
                {"urlname":"g-%s","name":"Bucharest JUG","categoryId":"%s","timeZone":"Europe/Bucharest",
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

    private UUID createEvent(String token, UUID groupId, UUID venueId, int capacity, boolean waitlist)
            throws Exception {
        String body = """
                {"title":"Spring Boot 4 Deep Dive","format":"IN_PERSON","venueId":"%s","startsAt":"%s",
                 "timeZone":"Europe/Bucharest","capacity":%d,"waitlistEnabled":%s,"guestsPerRsvpLimit":0,
                 "feeAmountMinor":2500,"feeCurrency":"EUR"}"""
                .formatted(venueId, Instant.now().plus(30, ChronoUnit.DAYS), capacity, waitlist);
        MvcResult result = mvc.perform(authed(post("/api/groups/" + groupId + "/events"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode event = read(result);
        assertThat(event.get("status").asString()).isEqualTo("DRAFT");
        return UUID.fromString(event.get("id").asString());
    }

    private org.springframework.test.web.servlet.ResultActions rsvpYes(String token, UUID eventId)
            throws Exception {
        return mvc.perform(authed(put("/api/events/" + eventId + "/rsvp"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"YES","guestCount":0}"""))
                .andExpect(status().isOk());
    }
}
