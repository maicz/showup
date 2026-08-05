package com.showup.api.exception;

/**
 * A domain rule refused the request — RSVPs closed, event cancelled, ticket revoked. Maps to 422:
 * the payload parsed and validated fine, the state machine just says no.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
