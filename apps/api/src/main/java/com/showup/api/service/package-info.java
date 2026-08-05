/**
 * Transactional business logic between {@link com.showup.api.controller} and
 * {@link com.showup.api.repository}. Services own the rules that outlive any one endpoint: seat
 * allocation and waitlist promotion, event state transitions, and the {@code GroupMemberRole}
 * checks in {@link com.showup.api.service.GroupAccessGuard}.
 *
 * <p>Services take the acting member as a {@code UUID} first parameter rather than reaching into
 * the security context, so the authorization rules are plain arguments — readable in a signature
 * and testable without a servlet.
 */
package com.showup.api.service;
