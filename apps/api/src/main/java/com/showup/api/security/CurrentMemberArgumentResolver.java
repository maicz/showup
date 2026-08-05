package com.showup.api.security;

import com.showup.api.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;
import java.util.UUID;

/**
 * Turns the {@code sub} claim of the validated bearer token into the {@link UUID} of the acting
 * member. A parameter typed {@code Optional<UUID>} resolves to empty for anonymous callers; a
 * bare {@code UUID} demands a token.
 */
@Component
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentMember.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Optional<UUID> memberId = currentMemberId();
        if (Optional.class.equals(parameter.getParameterType())) {
            return memberId;
        }
        return memberId.orElseThrow(() -> new UnauthorizedException("authentication required"));
    }

    private static Optional<UUID> currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(jwt.getSubject()));
    }
}
