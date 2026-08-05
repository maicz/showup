package com.showup.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cross-field rules that no single-field annotation can express: venue required unless
 * {@code ONLINE}, {@code onlineUrl} required unless {@code IN_PERSON}, {@code endsAt} after
 * {@code startsAt}, {@code timeZone} a real IANA id. See docs/domain-model.md#requests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEventRequestValidator.class)
public @interface ValidEventRequest {

    String message() default "venue is required unless the format is ONLINE, onlineUrl is required "
            + "unless the format is IN_PERSON, endsAt must be after startsAt, and timeZone must be a "
            + "valid IANA zone id";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
