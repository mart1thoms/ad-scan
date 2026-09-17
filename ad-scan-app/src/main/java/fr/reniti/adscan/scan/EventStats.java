package fr.reniti.adscan.scan;

import java.util.Map;

/**
 * Live statistics of an event, computed from its validated entries (one per member). Counts are
 * absolute; percentages are derived on the client so the same payload serves every view.
 *
 * @param totalEntries   members whose entry was validated
 * @param pendingPayment scans refused because the membership wasn't confirmed (distinct members)
 * @param duplicateScans re-scans of an already admitted badge
 * @param ensim          ENSIM students among the entries
 * @param personnel      ENSIM staff among the entries
 * @param exterieur      outsiders among the entries
 * @param unknownOrigin  entries with no formation recorded
 * @param years          entries per study year ("1A".."5A"), students only
 * @param filieres       entries per filière ("A&I" / "Info"), 3A-5A students only
 * @param alternants     3A-5A students in apprenticeship
 * @param nonAlternants  3A-5A students not in apprenticeship
 */
public record EventStats(
        boolean closed,
        String closedAt,
        int totalEntries,
        int pendingPayment,
        int duplicateScans,
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
