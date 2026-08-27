package com.showup.api.controller;

import com.showup.api.config.SecurityConfig;
import com.showup.api.dto.CategorySummary;
import com.showup.api.dto.GroupDetail;
import com.showup.api.dto.GroupSummary;
import com.showup.api.dto.PageResponse;
import com.showup.api.enums.GroupJoinPolicy;
import com.showup.api.enums.GroupStatus;
import com.showup.api.enums.GroupVisibility;
import com.showup.api.security.ApiErrorAuthenticationHandler;
import com.showup.api.service.EventService;
import com.showup.api.service.GroupService;
import com.showup.api.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A {@code @WebMvcTest} slice that additionally imports the real {@link SecurityConfig} — still
 * far short of a full {@code @SpringBootTest}: no repositories, no Testcontainers, no service
 * beans beyond the mocks below. Only the beans authorization actually needs are present: the JWT
 * resource-server filter, {@link ApiErrorAuthenticationHandler} (imported explicitly, since it is
 * a plain {@code @Component} that {@code @WebMvcTest} would not otherwise pick up), and
 * {@link GroupController} itself.
 *
 * <p>This is what lets the test assert the real 401/403 behavior — not a stand-in — while still
 * running as a fast, dependency-free slice.
 */
@WebMvcTest(GroupController.class)
@Import({SecurityConfig.class, ApiErrorAuthenticationHandler.class})
class GroupControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GroupService groups;
    @MockitoBean
    private VenueService venues;
    @MockitoBean
    private EventService events;

    @Test
    void creatingAGroupWithoutABearerTokenIsRejectedByTheRealFilterChain() throws Exception {
        mvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urlname\":\"x\",\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("a valid bearer token is required"));
    }

    @Test
    void aValidBearerTokenReachesTheControllerAndTheServiceSeesTheSubjectAsTheActor() throws Exception {
        UUID actorId = UUID.randomUUID();
        GroupDetail detail = new GroupDetail(UUID.randomUUID(), "backend-ro", "Backend RO",
                null, new CategorySummary(UUID.randomUUID(), "tech", "Technology", null, 1),
                null, null, null, "Europe/Bucharest", GroupVisibility.PUBLIC, GroupJoinPolicy.OPEN,
                1, null, 0, Instant.now(), GroupStatus.ACTIVE, List.of(), null, null, null);
        when(groups.create(eq(actorId), any())).thenReturn(detail);

        mvc.perform(post("/api/groups")
                        .with(jwt().jwt(token -> token.subject(actorId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"urlname":"backend-ro","name":"Backend RO","categoryId":"%s",
                                 "timeZone":"Europe/Bucharest","visibility":"PUBLIC","joinPolicy":"OPEN"}"""
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urlname").value("backend-ro"));
    }

    @Test
    void browsingGroupsRequiresNoTokenAtAllEvenInsideTheSecuredSlice() throws Exception {
        when(groups.list(0, 20, null, null, null, null))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0));

        mvc.perform(get("/api/groups")).andExpect(status().isOk());
    }
}
