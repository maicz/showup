package com.showup.api.exception;

import java.util.UUID;

/** The addressed resource does not exist. Maps to 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String what, UUID id) {
        return new NotFoundException(what + " " + id + " not found");
    }

    public static NotFoundException of(String what, String key) {
        return new NotFoundException(what + " '" + key + "' not found");
    }
}
