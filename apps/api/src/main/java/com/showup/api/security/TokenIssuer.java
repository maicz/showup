package com.showup.api.security;

import com.showup.api.config.SecurityProperties;
import com.showup.api.dto.TokenResponse;
import com.showup.api.entity.Member;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Mints the access token. The {@code sub} claim is the member id, which is what
 * {@link CurrentMemberArgumentResolver} hands to controllers; {@code email} and {@code name} ride
 * along so a client can render a header without a second call.
 */
@Component
public class TokenIssuer {

    private final JwtEncoder encoder;
    private final SecurityProperties properties;

    TokenIssuer(JwtEncoder encoder, SecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public TokenResponse issue(Member member) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("showup")
                .issuedAt(now)
                .expiresAt(now.plus(properties.jwt().ttl()))
                .subject(member.getId().toString())
                .claim("email", member.getEmail())
                .claim("name", member.getDisplayName())
                .build();
        // NimbusJwtEncoder defaults its header to RS256; with a shared secret the algorithm has
        // to be stated explicitly or encoding fails at runtime.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", properties.jwt().ttl().toSeconds());
    }
}
