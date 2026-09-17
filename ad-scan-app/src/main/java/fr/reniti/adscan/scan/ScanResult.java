package fr.reniti.adscan.scan;

public record ScanResult(
        ScanStatus status,
        String memberName,
        String memberEmail,
        String memberProvenance,
        String memberStartDate,
        String memberEndDate,
        Integer scanCount,
        /** Number of validated entries for the event after this scan (drives the live counter). */
        int totalEntries,
        String message
) {
}
