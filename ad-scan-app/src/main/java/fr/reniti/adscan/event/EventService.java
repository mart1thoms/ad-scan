package fr.reniti.adscan.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Stops accepting entries. Idempotent: closing an already closed event keeps the original closing time. */
    @Transactional
    public void close(Integer id) {
        Event event = eventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Événement introuvable"));
        if (!event.isClosed()) {
            eventRepository.updateClosedAt(id, LocalDateTime.now());
        }
    }

    @Transactional
    public void reopen(Integer id) {
        eventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Événement introuvable"));
        eventRepository.updateClosedAt(id, null);
    }
}
