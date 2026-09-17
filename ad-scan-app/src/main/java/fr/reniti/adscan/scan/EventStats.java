package fr.reniti.adscan.scan;

import java.util.List;
import java.util.Map;

/**
 * Live statistics of an event. Three breakdowns of the same shape let the client switch between
 * "everyone", "members" (badge scans) and "walk-ins" (entries typed in by hand at the door).
 */
public record EventStats(
        boolean closed,
        String closedAt,
        /** Members + walk-ins. */
        int totalEntries,
        /** Scans refused because the membership wasn't confirmed (distinct members, not admitted since). */
        int pendingPayment,
        /** Re-scans of an already admitted badge. */
        int duplicateScans,
        Breakdown all,
        Breakdown members,
        Breakdown walkIns,
        /** Walk-in entries grouped by exact category, for the +/- adjustment panel. */
        List<ManualCategory> manualCategories
) {

    /**
     * @param total         entries in this breakdown
     * @param ensim         ENSIM students
     * @param personnel     ENSIM staff
     * @param exterieur     outsiders
     * @param unknownOrigin entries with no formation recorded
     * @param years         entries per study year ("1A".."5A"), students only
     * @param filieres      entries per filière ("A&I" / "Info"), 3A-5A students only
     * @param alternants    3A-5A students in apprenticeship
     * @param nonAlternants 3A-5A students not in apprenticeship
     */
    public record Breakdown(
            int total,
            int ensim,
            int personnel,
            int exterieur,
            int unknownOrigin,
            Map<String, Integer> years,
            Map<String, Integer> filieres,
            int alternants,
            int nonAlternants
    ) {
    }

    public record ManualCategory(String formation, String filiere, Boolean alternant, String label, int count) {
    }
}
