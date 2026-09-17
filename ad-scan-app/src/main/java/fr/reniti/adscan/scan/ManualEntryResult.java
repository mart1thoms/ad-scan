package fr.reniti.adscan.scan;

public record ManualEntryResult(boolean accepted, String label, int totalEntries, String message) {
}
