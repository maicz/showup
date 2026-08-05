package com.showup.api.exception;

/** Bad credentials on the auth endpoints. Maps to 401, and never says which half was wrong. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
