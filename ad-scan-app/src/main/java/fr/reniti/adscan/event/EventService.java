package fr.reniti.adscan.event;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    public Optional<Event> findById(Integer id) {
        return eventRepository.findById(id);
    }

    public Event create(String name, LocalDate eventDate) {
        return eventRepository.insert(name, eventDate);
    }
}
