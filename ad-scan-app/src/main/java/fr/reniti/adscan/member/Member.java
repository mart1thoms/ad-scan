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
        LocalDate startDate,
        LocalDate endDate,
        String accessToken,
        boolean confirmed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
