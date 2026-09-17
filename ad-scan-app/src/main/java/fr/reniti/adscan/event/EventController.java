package fr.reniti.adscan.event;

import fr.reniti.adscan.scan.ScanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
public class EventController {

    private final EventService eventService;
    private final ScanService scanService;

    public EventController(EventService eventService, ScanService scanService) {
        this.eventService = eventService;
        this.scanService = scanService;
    }

    @GetMapping("/events")
    public String list(Model model) {
        model.addAttribute("events", eventService.findAll());
        return "event/list";
    }

    @GetMapping("/events/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new NewEventForm());
        }
        return "event/form";
    }

    @PostMapping("/events")
    public String create(@ModelAttribute("form") NewEventForm form) {
        eventService.create(form.getName(), form.getEventDate());
        return "redirect:/events";
    }

    @GetMapping("/events/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Event event = eventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));
        model.addAttribute("event", event);
        model.addAttribute("history", scanService.history(id));
        return "event/detail";
    }

    @GetMapping("/events/{id}/scan")
    public String scanPage(@PathVariable Integer id, Model model) {
        Event event = eventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));
        model.addAttribute("event", event);
        return "scan/scan";
    }
}
