package com.showup.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends ApiIntegrationTest {

    @Test
    void registerIssuesAUsableToken() throws Exception {
        String token = register("organizer");

        mvc.perform(authed(get("/api/members/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("organizer"))
                // The profile DTO has no such field, which is the point of not returning entities.
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginRejectsAWrongPasswordWithoutSayingWhichHalfWasWrong() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery","displayName":"Ana"}"""
                                .formatted(email)))
                .andExpect(status().isCreated());

        MvcResult wrongPassword = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"not-the-password"}""".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult noSuchAccount = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody-%s@example.test","password":"not-the-password"}"""
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(read(wrongPassword).get("message").asString())
                .isEqualTo(read(noSuchAccount).get("message").asString());
    }

    @Test
    void duplicateEmailIsAConflict() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.test";
        String body = """
                {"email":"%s","password":"correct-horse-battery","displayName":"Ana"}""".formatted(email);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void protectedEndpointsAnswerWithTheSameErrorShapeAsEverythingElse() throws Exception {
        MvcResult result = mvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode error = read(result);
        assertThat(error.get("status").asInt()).isEqualTo(401);
        assertThat(error.get("path").asString()).isEqualTo("/api/members/me");
        assertThat(error.has("timestamp")).isTrue();
    }

    @Test
    void validationFailuresComeBackFieldByField() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","displayName":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }

    @Test
    void ssoSaysNotImplementedRatherThanTrustingAnUnverifiedToken() throws Exception {
        mvc.perform(post("/api/auth/sso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"GOOGLE","idToken":"anything-at-all"}"""))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void browsingIsAnonymous() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/events")).andExpect(status().isOk());
    }
}
