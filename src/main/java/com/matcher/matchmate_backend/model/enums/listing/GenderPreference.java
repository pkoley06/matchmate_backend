package com.matcher.matchmate_backend.model.enums.listing;

import com.matcher.matchmate_backend.model.enums.auth.Gender;

/**
 * Who a host wants to share the room with. {@link #ALL} means open to every gender;
 * the other values mirror the selectable chips in the create-room wizard.
 */
public enum GenderPreference {
    ALL,
    MALE,
    FEMALE,
    NON_BINARY,
    OTHER;

    public boolean isOpenToAll() {
        return this == ALL;
    }

    /**
     * Bridges clients that still send the legacy {@code openToAllGenders} flag.
     * {@code true} (or unknown) maps to {@link #ALL}; {@code false} maps to the host's
     * own gender when it is one of the selectable preferences, otherwise {@link #ALL}.
     */
    public static GenderPreference fromLegacy(Boolean openToAllGenders, Gender hostGender) {
        if (openToAllGenders == null || openToAllGenders) return ALL;
        if (hostGender == null) return ALL;
        return switch (hostGender) {
            case MALE -> MALE;
            case FEMALE -> FEMALE;
            case NON_BINARY -> NON_BINARY;
            case OTHERS -> OTHER;
        };
    }
}
