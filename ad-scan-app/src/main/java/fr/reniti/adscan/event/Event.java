package fr.reniti.adscan.event;

import java.time.LocalDate;

public record Event(Integer id, String name, LocalDate eventDate) {
}
