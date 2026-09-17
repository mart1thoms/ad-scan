package fr.reniti.adscan.member;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Member(
        String id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String formation,
        String filiere,
        Boolean alternant,
        LocalDate startDate,
        LocalDate endDate,
        String accessToken,
        boolean confirmed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public String fullName() {
        return firstName + " " + lastName;
    }

    /** True once the membership end date is in the past (the member is due for automatic removal). */
    public boolean isExpired(LocalDate today) {
        return endDate != null && endDate.isBefore(today);
    }

    /**
     * Human-readable origin shown on the scan screen, e.g. "Étudiant ENSIM · 3A · Info · Alternant",
     * "Personnel ENSIM" or "Extérieur".
     */
    public String provenance() {
        if (formation == null || formation.isBlank()) {
            return "Provenance inconnue";
        }
        if (!FormationOptions.isStudent(formation)) {
            return formation;
        }
        StringBuilder label = new StringBuilder("Étudiant ENSIM · ").append(formation);
        if (filiere != null && !filiere.isBlank()) {
            label.append(" · ").append(filiere);
        }
        if (Boolean.TRUE.equals(alternant)) {
            label.append(" · Alternant");
        }
        return label.toString();
    }
}
