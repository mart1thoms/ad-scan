package fr.reniti.adscan.scan;

import fr.reniti.adscan.event.Event;
import fr.reniti.adscan.event.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ScanController {

    private final ScanService scanService;
    private final EventService eventService;

    public ScanController(ScanService scanService, EventService eventService) {
        this.scanService = scanService;
        this.eventService = eventService;
    }

    @PostMapping("/api/events/{eventId}/scan")
    public ScanResult scan(@PathVariable Integer eventId, @RequestBody ScanRequest request) {
        Event event = findEventOrThrow(eventId);
        if (request.key() == null || request.key().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clé de scan manquante");
        }
        return scanService.scan(event, request.key().trim());
    }

    /** Walk-in (no badge) entry typed in at the door. */
    @PostMapping("/api/events/{eventId}/manual-entries")
    public ManualEntryResult addManualEntry(@PathVariable Integer eventId, @RequestBody ManualEntryRequest request) {
        return scanService.addManualEntry(findEventOrThrow(eventId), request);
    }

    /** Undo / adjustment: removes the most recent walk-in entry of that category. */
    @PostMapping("/api/events/{eventId}/manual-entries/remove")
    public ManualEntryResult removeManualEntry(@PathVariable Integer eventId, @RequestBody ManualEntryRequest request) {
        return scanService.removeManualEntry(findEventOrThrow(eventId), request);
    }

    /** Live statistics, polled by the scan page and the event page. */
    @GetMapping("/api/events/{eventId}/stats")
    public EventStats stats(@PathVariable Integer eventId) {
        return scanService.stats(findEventOrThrow(eventId));
    }

    private Event findEventOrThrow(Integer eventId) {
        return eventService.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));
    }
}
