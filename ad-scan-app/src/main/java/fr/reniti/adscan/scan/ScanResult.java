package fr.reniti.adscan.scan;

public record ScanResult(
        ScanStatus status,
        String memberName,
        String memberEmail,
        String memberStartDate,
        String memberEndDate,
        Integer scanCount,
        String message
) {
}
