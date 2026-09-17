package fr.reniti.adscan.scan;

import java.time.LocalDateTime;

public record ScanLogEntry(LocalDateTime scannedAt, String memberName, String status, String scannedKey) {
}
