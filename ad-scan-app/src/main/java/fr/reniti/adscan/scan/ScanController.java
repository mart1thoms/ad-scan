package fr.reniti.adscan.scan;

import fr.reniti.adscan.event.EventService;
import org.springframework.http.HttpStatus;
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
        eventService.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));
        if (request.key() == null || request.key().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clé de scan manquante");
        }
        return scanService.scan(eventId, request.key().trim());
    }
}
