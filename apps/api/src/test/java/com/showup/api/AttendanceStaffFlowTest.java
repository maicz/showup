package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Staffing and the door: {@code StaffRole.SCANNER} is the one role delegated outside the
 * organizer roles, and ticket/check-in rules that a single happy-path scan does not exercise.
 */
class AttendanceStaffFlowTest extends ApiIntegrationTest {

    @Test
    void anAssignedScannerCanCheckInWithoutBeingAnOrganizer() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        String volunteer = register("volunteer");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());
        rsvpYes(attendee, eventId);
        String ticketCode = issueTicket(attendee, eventId);
        UUID volunteerId = memberId(volunteer);
        join(volunteer, groupId);

        MvcResult assignment = mvc.perform(authed(post("/api/events/" + eventId + "/staff"), organizer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"" + volunteerId + "\",\"role\":\"SCANNER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID assignmentId = UUID.fromString(read(assignment).get("id").asString());

        // Duplicate assignment of the same role to the same member is a conflict.
        mvc.perform(authed(post("/api/events/" + eventId + "/staff"), organizer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"" + volunteerId + "\",\"role\":\"SCANNER\"}"))
                .andExpect(status().isConflict());

        String checkInBody = "{\"ticketCode\":\"" + ticketCode + "\",\"method\":\"QR_SCAN\",\"admittedCount\":1}";
        mvc.perform(authed(post("/api/events/" + eventId + "/check-ins"), volunteer)
                        .contentType(MediaType.APPLICATION_JSON).content(checkInBody))
                .andExpect(status().isOk());

        mvc.perform(authed(get("/api/events/" + eventId + "/staff"), organizer))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(authed(delete("/api/events/" + eventId + "/staff/" + assignmentId), organizer))
                .andExpect(status().isNoContent());
        mvc.perform(authed(get("/api/events/" + eventId + "/staff"), organizer))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void neitherAnOutsiderNorARemovedScannerMayWorkTheDoor() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        String outsider = register("outsider");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());
        rsvpYes(attendee, eventId);
        String ticketCode = issueTicket(attendee, eventId);

        String checkInBody = "{\"ticketCode\":\"" + ticketCode + "\",\"method\":\"QR_SCAN\",\"admittedCount\":1}";
        mvc.perform(authed(post("/api/events/" + eventId + "/check-ins"), outsider)
                        .contentType(MediaType.APPLICATION_JSON).content(checkInBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanOnlyBeAssignedFromExistingGroupMembers() throws Exception {
        String organizer = register("organizer");
        String outsider = register("outsider");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        UUID outsiderId = memberId(outsider);

        mvc.perform(authed(post("/api/events/" + eventId + "/staff"), organizer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"" + outsiderId + "\",\"role\":\"GREETER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aRevokedTicketCannotBeUsedForCheckIn() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());
        rsvpYes(attendee, eventId);
        MvcResult ticket = mvc.perform(authed(post("/api/events/" + eventId + "/ticket"), attendee))
                .andExpect(status().isOk()).andReturn();
        JsonNode ticketJson = read(ticket);
        UUID ticketId = UUID.fromString(ticketJson.get("id").asString());
        String code = ticketJson.get("code").asString();

        mvc.perform(authed(post("/api/events/" + eventId + "/tickets/" + ticketId + "/revoke"), organizer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revoked").value(true));

        String checkInBody = "{\"ticketCode\":\"" + code + "\",\"method\":\"QR_SCAN\",\"admittedCount\":1}";
        mvc.perform(authed(post("/api/events/" + eventId + "/check-ins"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(checkInBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void checkInRefusesToAdmitMoreThanTheTicketAllowsAndRefusesACancelledEvent() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());
        rsvpYes(attendee, eventId);
        String code = issueTicket(attendee, eventId);

        String tooMany = "{\"ticketCode\":\"" + code + "\",\"method\":\"QR_SCAN\",\"admittedCount\":2}";
        mvc.perform(authed(post("/api/events/" + eventId + "/check-ins"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(tooMany))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(authed(post("/api/events/" + eventId + "/cancel"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"reason":"weather"}"""))
                .andExpect(status().isOk());
        String ok = "{\"ticketCode\":\"" + code + "\",\"method\":\"QR_SCAN\",\"admittedCount\":1}";
        mvc.perform(authed(post("/api/events/" + eventId + "/check-ins"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aTicketCannotBeScannedIntoADifferentEvent() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventOne = createEvent(organizer, groupId, venueId);
        UUID eventTwo = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventOne + "/publish"), organizer)).andExpect(status().isOk());
        mvc.perform(authed(post("/api/events/" + eventTwo + "/publish"), organizer)).andExpect(status().isOk());
        rsvpYes(attendee, eventOne);
        String codeForEventOne = issueTicket(attendee, eventOne);

        String checkInBody = "{\"ticketCode\":\"" + codeForEventOne + "\",\"method\":\"QR_SCAN\",\"admittedCount\":1}";
        mvc.perform(authed(post("/api/events/" + eventTwo + "/check-ins"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(checkInBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void theOrganizerOnlyAttendeeRosterListsWhoIsSeated() throws Exception {
        String organizer = register("organizer");
        String attendee = register("attendee");
        UUID groupId = createGroup(organizer);
        UUID venueId = createVenue(organizer, groupId);
        UUID eventId = createEvent(organizer, groupId, venueId);
        mvc.perform(authed(post("/api/events/" + eventId + "/publish"), organizer)).andExpect(status().isOk());
        rsvpYes(attendee, eventId);

        mvc.perform(authed(get("/api/events/" + eventId + "/attendees"), organizer).param("status", "YES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(authed(get("/api/events/" + eventId + "/attendees"), attendee))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private UUID createGroup(String token) throws Exception {
        String body = """
                {"urlname":"as-%s","name":"Attendance Group","categoryId":"%s","timeZone":"Europe/Bucharest",
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
                {"title":"Door Test","format":"IN_PERSON","venueId":"%s","startsAt":"%s",
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
        mvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/events/" + eventId + "/rsvp"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"YES","guestCount":0}"""))
                .andExpect(status().isOk());
    }

    private String issueTicket(String token, UUID eventId) throws Exception {
        MvcResult result = mvc.perform(authed(post("/api/events/" + eventId + "/ticket"), token))
                .andExpect(status().isOk()).andReturn();
        return read(result).get("code").asString();
    }

    private UUID memberId(String token) throws Exception {
        MvcResult result = mvc.perform(authed(get("/api/members/me"), token)).andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }

    private void join(String token, UUID groupId) throws Exception {
        mvc.perform(authed(post("/api/groups/" + groupId + "/members"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
    }
}
