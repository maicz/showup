package com.showup.api.service;

import com.showup.api.dto.LoginRequest;
import com.showup.api.dto.RegisterRequest;
import com.showup.api.dto.SsoLoginRequest;
import com.showup.api.dto.TokenResponse;
import com.showup.api.entity.Member;
import com.showup.api.enums.MemberStatus;
import com.showup.api.exception.ConflictException;
import com.showup.api.exception.ForbiddenException;
import com.showup.api.exception.NotImplementedException;
import com.showup.api.exception.UnauthorizedException;
import com.showup.api.repository.MemberRepository;
import com.showup.api.security.TokenIssuer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    /** Deliberately identical for "no such email" and "wrong password" — the difference is a user oracle. */
    private static final String BAD_CREDENTIALS = "invalid email or password";

    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokens;

    AuthService(MemberRepository members, PasswordEncoder passwordEncoder, TokenIssuer tokens) {
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    public TokenResponse register(RegisterRequest request) {
        if (members.existsByEmail(request.email())) {
            throw new ConflictException("an account already exists for that email");
        }
        Member member = new Member(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName());
        return tokens.issue(members.save(member));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = members.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS));
        // An SSO-only account has no hash; matches() against null would throw rather than deny.
        if (member.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("account is " + member.getStatus());
        }
        return tokens.issue(member);
    }

    /**
     * Not implemented, and deliberately not faked.
     *
     * <p>Honouring this would mean trusting {@code idToken}, which is only safe after verifying
     * the provider's signature against its JWKS, plus the issuer, audience, and expiry. Accepting
     * the token's {@code sub} without that check is an authentication bypass: anyone could mint a
     * token naming any subject. The {@code member_identity} table and the lookup path are already
     * modelled; what is missing is the per-provider verification, which is a subsystem of its own.
     */
    public TokenResponse ssoLogin(SsoLoginRequest request) {
        throw new NotImplementedException(
                "SSO login for " + request.provider() + " needs provider token verification (JWKS, "
                        + "issuer, audience) before it can be trusted; not wired up yet");
    }
}
