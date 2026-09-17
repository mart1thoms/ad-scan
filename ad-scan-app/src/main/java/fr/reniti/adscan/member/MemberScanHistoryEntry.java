package fr.reniti.adscan.member;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberScanHistoryEntry(String eventName, LocalDate eventDate, LocalDateTime scannedAt, String status) {
}
