package com.matcher.matchmate_backend.model.enums.auth;

public enum PrimaryAuthProvider {
    GOOGLE,
    PHONE,
    /** Created directly by an admin via the admin console - has no real Firebase identity. */
    ADMIN_CREATED
}
