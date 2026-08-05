package com.showup.api.exception;

/**
 * The request collides with existing state — a duplicate urlname, a second RSVP, a re-used email.
 * Maps to 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
