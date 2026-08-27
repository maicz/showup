package com.showup.api.service;

import com.showup.api.config.SecurityProperties;
import com.showup.api.dto.ForgotPasswordRequest;
import com.showup.api.dto.LoginRequest;
import com.showup.api.dto.MessageResponse;
import com.showup.api.dto.RegisterRequest;
import com.showup.api.dto.ResetPasswordRequest;
import com.showup.api.dto.SsoLoginRequest;
import com.showup.api.dto.TokenResponse;
import com.showup.api.entity.Member;
import com.showup.api.enums.IdentityProvider;
import com.showup.api.enums.MemberStatus;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ConflictException;
import com.showup.api.exception.NotImplementedException;
import com.showup.api.exception.UnauthorizedException;
import com.showup.api.repository.MemberIdentityRepository;
import com.showup.api.repository.MemberRepository;
import com.showup.api.security.TokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository members;
    @Mock
    private MemberIdentityRepository identities;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenIssuer tokens;
    @Mock
    private JwtEncoder jwtEncoder;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private NotificationService notifications;

    private AuthService service;

    @BeforeEach
    void setUp() {
        SecurityProperties props = new SecurityProperties(
                new SecurityProperties.Jwt("very-secure-test-secret-at-least-32-bytes-long", Duration.ofHours(12)));
        service = new AuthService(members, identities, passwordEncoder, tokens, jwtEncoder, jwtDecoder, notifications, props);
    }

    @Test
    void registerWithExistingEmailThrowsConflict() {
        when(members.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("test@example.com", "Secret123!", "Test User")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginWithWrongPasswordThrowsUnauthorized() {
        Member member = new Member("test@example.com", "hashed_pwd", "Test User");
        when(members.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("WrongPwd!", "hashed_pwd")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("test@example.com", "WrongPwd!")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void forgotPasswordReturnsHelpfulMessageAndDispatchesNotification() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(UUID.randomUUID());
        when(member.getEmail()).thenReturn("test@example.com");
        when(members.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("mocked_token");
        when(jwtEncoder.encode(any())).thenReturn(jwt);

        MessageResponse response = service.forgotPassword(new ForgotPasswordRequest("test@example.com"));

        assertThat(response.message()).contains("password reset instructions have been sent");
        verify(notifications).sendPasswordResetEmail(any(), any());
    }

    @Test
    void ssoLoginRejectsUnverifiedProviderTokens() {
        assertThatThrownBy(() -> service.ssoLogin(
                new SsoLoginRequest(IdentityProvider.GOOGLE, "google_id_token_12345")))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void resetPasswordSuccessfullySetsNewPasswordAndRecordsTimestamp() {
        UUID memberId = UUID.randomUUID();
        Member member = new Member("test@example.com", "old_hash", "Test User");
        when(members.findById(memberId)).thenReturn(Optional.of(member));

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("purpose")).thenReturn("reset-password");
        when(jwt.getSubject()).thenReturn(memberId.toString());
        when(jwt.getIssuedAt()).thenReturn(java.time.Instant.now());
        when(jwtDecoder.decode("valid_token")).thenReturn(jwt);
        when(passwordEncoder.encode("NewSecret123!")).thenReturn("new_hash");

        MessageResponse response = service.resetPassword(new ResetPasswordRequest("valid_token", "NewSecret123!"));

        assertThat(response.message()).contains("Password has been reset successfully");
        assertThat(member.getPasswordHash()).isEqualTo("new_hash");
        assertThat(member.getPasswordUpdatedAt()).isNotNull();
    }

    @Test
    void resetPasswordRejectsAlreadyUsedToken() {
        UUID memberId = UUID.randomUUID();
        Member member = new Member("test@example.com", "current_hash", "Test User");
        java.time.Instant passwordResetTime = java.time.Instant.now();
        member.setPasswordUpdatedAt(passwordResetTime);
        when(members.findById(memberId)).thenReturn(Optional.of(member));

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("purpose")).thenReturn("reset-password");
        when(jwt.getSubject()).thenReturn(memberId.toString());
        // Token was issued before the member's last password update
        when(jwt.getIssuedAt()).thenReturn(passwordResetTime.minusSeconds(60));
        when(jwtDecoder.decode("used_token")).thenReturn(jwt);

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("used_token", "AnotherSecret123!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been used");
    }
}
