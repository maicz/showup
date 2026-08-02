package com.showup.api.attendance;

/** {@link #SCANNER} is the role the check-in endpoint authorizes against, alongside group organizers. */
public enum StaffRole {
    GREETER,
    SCANNER,
    SETUP,
    AV,
    SPEAKER_LIAISON,
    CLEANUP
}
