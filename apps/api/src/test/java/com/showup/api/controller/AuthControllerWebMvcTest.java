package com.showup.api.controller;

import com.showup.api.dto.MessageResponse;
import com.showup.api.dto.TokenResponse;
import com.showup.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService auth;

    @Test
    void registerReturnsTokenResponse() throws Exception {
        TokenResponse token = new TokenResponse("test_token", "Bearer", 3600);
        when(auth.register(any())).thenReturn(token);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"SecurePassword123!\",\"displayName\":\"Alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("test_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void forgotPasswordReturnsSuccessMessage() throws Exception {
        when(auth.forgotPassword(any())).thenReturn(new MessageResponse("Instructions sent"));

        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Instructions sent"));
    }
}
