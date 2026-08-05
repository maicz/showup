package com.showup.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the authenticated member's id to a controller parameter:
 * {@code create(@CurrentMember UUID actor, ...)}.
 *
 * <p>Sugar over {@code @AuthenticationPrincipal Jwt} plus a {@code UUID.fromString(getSubject())}
 * in every handler. Resolved by {@link CurrentMemberArgumentResolver}. Declare the parameter as
 * {@code Optional<UUID>} on endpoints that are readable anonymously but richer when signed in.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {
}
