package fr.reniti.adscan.scan;

public enum ScanStatus {
    /** Entry validated and counted. */
    OK,
    /** Badge already scanned for this event. */
    DUPLICATE,
    UNKNOWN_KEY,
    TOKEN_INVALIDATED,
    /** Member not yet confirmed by staff: shown in orange, NOT counted as an entry. */
    PENDING_PAYMENT,
    /** Membership end date is in the past. */
    EXPIRED,
    /** The event has been closed: no more entries accepted. Not written to scan_log. */
    EVENT_CLOSED
}
