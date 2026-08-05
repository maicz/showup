package com.showup.api;

import com.showup.api.config.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Shared setup for the HTTP-level tests. One set of annotations across every subclass so Spring
 * reuses a single application context — and a single Postgres container — for the whole suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class ApiIntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** Registers a fresh member and returns the bearer token. Emails are unique per call. */
    protected String register(String prefix) throws Exception {
        String body = """
                {"email":"%s-%s@example.test","password":"correct-horse-battery","displayName":"%s"}"""
                .formatted(prefix, UUID.randomUUID(), prefix);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return read(result).get("accessToken").asString();
    }

    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected UUID firstCategoryId() throws Exception {
        MvcResult result = mvc.perform(get("/api/categories")).andReturn();
        return UUID.fromString(read(result).get(0).get("id").asString());
    }
}
