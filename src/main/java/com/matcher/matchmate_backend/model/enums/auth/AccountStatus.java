package com.matcher.matchmate_backend.model.enums.auth;

/**
 * Lifecycle of a self-service account.
 * <ul>
 *   <li>{@code ACTIVE} - normal.</li>
 *   <li>{@code DISABLED} - the user paused their account (POST /users/me/disable). Hidden from
 *       discovery, searches and listing visibility; every bearer token is rejected; signing in
 *       again re-activates it.</li>
 *   <li>{@code DELETED} - the user deleted their account (DELETE /users/me). Permanent.</li>
 * </ul>
 */
public enum AccountStatus {
    ACTIVE,
    DISABLED,
    DELETED
}
