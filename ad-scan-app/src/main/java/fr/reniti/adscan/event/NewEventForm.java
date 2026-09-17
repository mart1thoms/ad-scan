package fr.reniti.adscan.event;

import java.time.LocalDate;

public class NewEventForm {

    private String name;
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
