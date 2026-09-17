package fr.reniti.adscan.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Event(Integer id, String name, LocalDate eventDate, LocalDateTime closedAt) {

    /** A closed event no longer accepts entries; its statistics stay available. */
    public boolean isClosed() {
        return closedAt != null;
    }
}
