/**
 * Domain exceptions the service layer throws to signal a failed rule. Each one maps to a single
 * HTTP status in {@code ApiExceptionHandler}, so services never mention {@code HttpStatus} or
 * {@code ResponseEntity}.
 */
package com.showup.api.exception;
