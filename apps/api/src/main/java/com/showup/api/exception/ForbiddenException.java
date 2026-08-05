package com.showup.api.exception;

/**
 * The caller is authenticated but lacks the role this action needs. Maps to 403.
 *
 * <p>Distinct from Spring Security's authentication failures, which are about <em>who</em> the
 * caller is; this is about what their {@code GroupMemberRole} permits.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
