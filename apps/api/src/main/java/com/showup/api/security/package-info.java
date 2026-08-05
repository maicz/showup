/**
 * Authentication plumbing: how a bearer token becomes an acting member id. Everything here is
 * infrastructure — the authorization <em>rules</em> (who may edit an event, who may scan a
 * ticket) live in {@link com.showup.api.service}, because they are a function of
 * {@code GroupMemberRole} rather than of the token.
 */
package com.showup.api.security;
