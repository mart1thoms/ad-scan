package fr.reniti.adscan.event;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class NewEventForm {

    private String name;
    // ISO so the value renders as yyyy-MM-dd, the only format an <input type=\"date\"> accepts.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventDate = LocalDate.now();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }
}
