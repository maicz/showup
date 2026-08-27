package com.showup.api.service;

import com.showup.api.config.SecurityProperties;
import com.showup.api.dto.ForgotPasswordRequest;
import com.showup.api.dto.LoginRequest;
import com.showup.api.dto.MessageResponse;
import com.showup.api.dto.RegisterRequest;
import com.showup.api.dto.ResetPasswordRequest;
import com.showup.api.dto.SsoLoginRequest;
import com.showup.api.dto.TokenResponse;
import com.showup.api.dto.VerifyEmailRequest;
import com.showup.api.entity.Member;
import com.showup.api.entity.MemberIdentity;
import com.showup.api.enums.IdentityProvider;
import com.showup.api.enums.MemberStatus;
import com.showup.api.exception.BusinessRuleException;
import com.showup.api.exception.ConflictException;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.exception.NotFoundException;
import com.showup.api.exception.NotImplementedException;
import com.showup.api.exception.UnauthorizedException;
import com.showup.api.repository.MemberIdentityRepository;
import com.showup.api.repository.MemberRepository;
import com.showup.api.security.TokenIssuer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final String BAD_CREDENTIALS = "invalid email or password";

    private final MemberRepository members;
    private final MemberIdentityRepository identities;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokens;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final NotificationService notifications;
    private final SecurityProperties properties;

    AuthService(MemberRepository members,
                MemberIdentityRepository identities,
                PasswordEncoder passwordEncoder,
                TokenIssuer tokens,
                JwtEncoder jwtEncoder,
                JwtDecoder jwtDecoder,
                NotificationService notifications,
                SecurityProperties properties) {
        this.members = members;
        this.identities = identities;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.notifications = notifications;
        this.properties = properties;
    }

    public TokenResponse register(RegisterRequest request) {
        if (members.existsByEmail(request.email())) {
            throw new ConflictException("an account already exists for that email");
        }
        Member member = new Member(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName());
        Member saved = members.save(member);

        String verifyToken = createActionToken(saved.getId(), "verify-email", Duration.ofHours(24));
        notifications.sendVerificationEmail(saved.getEmail(), verifyToken);

        return tokens.issue(saved);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = members.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS));
        if (member.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("account is " + member.getStatus());
        }
        return tokens.issue(member);
    }

    public TokenResponse ssoLogin(SsoLoginRequest request) {
        throw new NotImplementedException(
                "social sign-in is unavailable until provider token verification is configured");
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        members.findByEmail(request.email()).ifPresent(member -> {
            String token = createActionToken(member.getId(), "reset-password", Duration.ofHours(2));
            notifications.sendPasswordResetEmail(member.getEmail(), token);
        });
        return new MessageResponse("If that email is registered, password reset instructions have been sent.");
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        UUID memberId = verifyActionToken(request.token(), "reset-password");
        Member member = members.findById(memberId)
                .orElseThrow(() -> new NotFoundException("member not found"));

        member.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return new MessageResponse("Password has been reset successfully. You may now sign in.");
    }

    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        UUID memberId = verifyActionToken(request.token(), "verify-email");
        Member member = members.findById(memberId)
                .orElseThrow(() -> new NotFoundException("member not found"));

        member.setEmailVerifiedAt(Instant.now());
        return new MessageResponse("Email verified successfully.");
    }

    public void resendVerification(UUID memberId) {
        Member member = members.findById(memberId)
                .orElseThrow(() -> new NotFoundException("member not found"));
        if (member.getEmailVerifiedAt() == null) {
            String token = createActionToken(member.getId(), "verify-email", Duration.ofHours(24));
            notifications.sendVerificationEmail(member.getEmail(), token);
        }
    }

    private String createActionToken(UUID memberId, String purpose, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("showup")
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(memberId.toString())
                .claim("purpose", purpose)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private UUID verifyActionToken(String token, String expectedPurpose) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String purpose = jwt.getClaimAsString("purpose");
            if (!expectedPurpose.equals(purpose)) {
                throw new BusinessRuleException("invalid token purpose");
            }
            return UUID.fromString(jwt.getSubject());
        } catch (Exception ex) {
            throw new BusinessRuleException("token is invalid or has expired");
        }
    }

}
