package fr.reniti.adscan.scan;

/** A walk-in (no badge) entry typed in at the door: "Extérieur", "Personnel ENSIM" or a student year. */
public record ManualEntryRequest(String formation, String filiere, Boolean alternant) {
}
