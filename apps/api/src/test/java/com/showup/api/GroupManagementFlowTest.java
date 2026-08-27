package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Group membership: join policies, approval, roles, and the "a group always has an organizer"
 * invariant that {@code GroupService} and {@code GroupAccessGuard} enforce together.
 */
class GroupManagementFlowTest extends ApiIntegrationTest {

    @Test
    void updatingAGroupReplacesItsTopics() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "OPEN");
        UUID topicId = firstTopicId();

        String body = """
                {"name":"Renamed Group","description":"new description","categoryId":"%s",
                 "city":"Cluj","country":"RO","timeZone":"Europe/Bucharest","visibility":"PRIVATE",
                 "joinPolicy":"APPROVAL_REQUIRED","topicIds":["%s"]}"""
                .formatted(firstCategoryId(), topicId);
        mvc.perform(authed(put("/api/groups/" + groupId), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Group"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.topics[0].id").value(topicId.toString()));
    }

    @Test
    void anApprovalRequiredGroupParksNewMembersUntilApproved() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "APPROVAL_REQUIRED");
        String applicant = register("applicant");

        MvcResult joined = mvc.perform(authed(post("/api/groups/" + groupId + "/members"), applicant)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID applicantId = UUID.fromString(read(joined).get("memberId").asString());
        assertStatus(joined, "PENDING_APPROVAL");

        // Not yet active, so the roster (default filter ACTIVE) does not list them.
        mvc.perform(authed(get("/api/groups/" + groupId + "/members"), organizer))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(authed(post("/api/groups/" + groupId + "/members/" + applicantId + "/approve"), organizer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(authed(get("/api/groups/" + groupId + "/members"), organizer))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void approvingATwiceApprovedMembershipIsRejected() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "APPROVAL_REQUIRED");
        String applicant = register("applicant");
        MvcResult joined = mvc.perform(authed(post("/api/groups/" + groupId + "/members"), applicant)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();
        UUID applicantId = UUID.fromString(read(joined).get("memberId").asString());

        mvc.perform(authed(post("/api/groups/" + groupId + "/members/" + applicantId + "/approve"), organizer))
                .andExpect(status().isOk());
        mvc.perform(authed(post("/api/groups/" + groupId + "/members/" + applicantId + "/approve"), organizer))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anInviteOnlyGroupRefusesASelfServeJoin() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "INVITE_ONLY");
        String outsider = register("outsider");

        mvc.perform(authed(post("/api/groups/" + groupId + "/members"), outsider)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void joiningTwiceIsAConflictAndLeavingThenRejoiningReusesTheRow() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "OPEN");
        String member = register("member");

        mvc.perform(authed(post("/api/groups/" + groupId + "/members"), member)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        mvc.perform(authed(post("/api/groups/" + groupId + "/members"), member)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());

        mvc.perform(authed(delete("/api/groups/" + groupId + "/members/me"), member))
                .andExpect(status().isNoContent());

        mvc.perform(authed(post("/api/groups/" + groupId + "/members"), member)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void aGroupMustAlwaysKeepAnActiveOrganizer() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "OPEN");

        mvc.perform(authed(delete("/api/groups/" + groupId + "/members/me"), organizer))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void promotingASecondOrganizerAllowsTheFirstToStepDownButNotBothAtOnce() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "OPEN");
        String member = register("member");
        mvc.perform(authed(post("/api/groups/" + groupId + "/members"), member)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();
        UUID organizerId = memberId(organizer);
        UUID memberIdValue = memberId(member);

        mvc.perform(authed(put("/api/groups/" + groupId + "/members/" + memberIdValue + "/role"), organizer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"role":"ORGANIZER"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"));

        // Now that two organizers exist, the original may step down to a plain member.
        mvc.perform(authed(put("/api/groups/" + groupId + "/members/" + organizerId + "/role"), member)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"role":"MEMBER"}"""))
                .andExpect(status().isOk());

        // But the sole remaining organizer cannot be demoted.
        mvc.perform(authed(put("/api/groups/" + groupId + "/members/" + memberIdValue + "/role"), member)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"role":"MEMBER"}"""))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aGroupIsAlsoReachableByItsUrlnameAndListingPages() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "OPEN");

        MvcResult byId = mvc.perform(get("/api/groups/" + groupId)).andReturn();
        String urlname = read(byId).get("urlname").asString();

        mvc.perform(get("/api/groups/by-urlname/" + urlname))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId.toString()));
        mvc.perform(get("/api/groups/by-urlname/no-such-group"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/groups").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void publicDiscoverySearchesOnTheServerInsteadOfOnlyTheLoadedPage() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "OPEN");
        String uniqueName = "Alpine Readers " + UUID.randomUUID();

        mvc.perform(authed(put("/api/groups/" + groupId), organizer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"Books above the clouds","categoryId":"%s",
                                 "city":"Cluj-Napoca","country":"RO","timeZone":"Europe/Bucharest",
                                 "visibility":"PUBLIC","joinPolicy":"OPEN"}"""
                                .formatted(uniqueName, firstCategoryId())))
                .andExpect(status().isOk());

        mvc.perform(get("/api/groups")
                        .param("query", uniqueName.toUpperCase())
                        .param("city", "  CLUJ-NAPOCA ")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(groupId.toString()));
    }

    @Test
    void privateGroupsStayOutOfDiscoveryAndAwayFromNonMembers() throws Exception {
        String organizer = register("organizer");
        UUID groupId = createGroup(organizer, "OPEN");
        String privateName = "Private Circle " + UUID.randomUUID();

        mvc.perform(authed(put("/api/groups/" + groupId), organizer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","categoryId":"%s","timeZone":"Europe/Bucharest",
                                 "visibility":"PRIVATE","joinPolicy":"INVITE_ONLY"}"""
                                .formatted(privateName, firstCategoryId())))
                .andExpect(status().isOk());

        mvc.perform(get("/api/groups/" + groupId)).andExpect(status().isNotFound());
        mvc.perform(authed(get("/api/groups/" + groupId), organizer)).andExpect(status().isOk());
        mvc.perform(get("/api/groups").param("query", privateName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // --- helpers ---

    private UUID createGroup(String token, String joinPolicy) throws Exception {
        String body = """
                {"urlname":"gm-%s","name":"Management Group","categoryId":"%s","timeZone":"Europe/Bucharest",
                 "visibility":"PUBLIC","joinPolicy":"%s"}"""
                .formatted(UUID.randomUUID().toString().substring(0, 8), firstCategoryId(), joinPolicy);
        MvcResult result = mvc.perform(authed(post("/api/groups"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }

    private UUID firstTopicId() throws Exception {
        MvcResult result = mvc.perform(get("/api/topics")).andReturn();
        return UUID.fromString(read(result).get(0).get("id").asString());
    }

    private UUID memberId(String token) throws Exception {
        MvcResult result = mvc.perform(authed(get("/api/members/me"), token)).andReturn();
        return UUID.fromString(read(result).get("id").asString());
    }

    private void assertStatus(MvcResult result, String expected) throws Exception {
        org.assertj.core.api.Assertions.assertThat(read(result).get("status").asString()).isEqualTo(expected);
    }
}
