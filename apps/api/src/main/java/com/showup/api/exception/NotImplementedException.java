package com.showup.api.exception;

/**
 * A modelled endpoint whose backing subsystem is deliberately absent. Maps to 501 so callers can
 * tell "not built yet" apart from "you did something wrong".
 */
public class NotImplementedException extends RuntimeException {

    public NotImplementedException(String message) {
        super(message);
    }
}
