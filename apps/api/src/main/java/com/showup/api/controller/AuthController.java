package com.showup.api.controller;

import com.showup.api.dto.ForgotPasswordRequest;
import com.showup.api.dto.LoginRequest;
import com.showup.api.dto.MessageResponse;
import com.showup.api.dto.RegisterRequest;
import com.showup.api.dto.ResetPasswordRequest;
import com.showup.api.dto.SsoLoginRequest;
import com.showup.api.dto.TokenResponse;
import com.showup.api.dto.VerifyEmailRequest;
import com.showup.api.security.CurrentMember;
import com.showup.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    @PostMapping("/sso")
    public TokenResponse sso(@Valid @RequestBody SsoLoginRequest request) {
        return auth.ssoLogin(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return auth.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return auth.resetPassword(request);
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return auth.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@CurrentMember UUID actor) {
        auth.resendVerification(actor);
    }
}
