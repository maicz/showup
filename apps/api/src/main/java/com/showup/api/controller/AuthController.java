package com.showup.api.controller;

import com.showup.api.dto.LoginRequest;
import com.showup.api.dto.RegisterRequest;
import com.showup.api.dto.SsoLoginRequest;
import com.showup.api.dto.TokenResponse;
import com.showup.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    /** Modelled but not honoured — returns 501 until provider token verification exists. */
    @PostMapping("/sso")
    public TokenResponse sso(@Valid @RequestBody SsoLoginRequest request) {
        return auth.ssoLogin(request);
    }
}
