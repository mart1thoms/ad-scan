package fr.reniti.adscan.event;

import fr.reniti.adscan.scan.ScanService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

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
        List<Event> events = eventService.findAll();
        model.addAttribute("events", events);
        model.addAttribute("entryCounts", scanService.entryCountsByEvent());
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
        Event event = findEventOrThrow(id);
        model.addAttribute("event", event);
        model.addAttribute("stats", scanService.stats(event));
        model.addAttribute("history", scanService.history(id));
        return "event/detail";
    }

    @GetMapping("/events/{id}/scan")
    public String scanPage(@PathVariable Integer id, Model model) {
        Event event = findEventOrThrow(id);
        model.addAttribute("event", event);
        model.addAttribute("stats", scanService.stats(event));
        return "scan/scan";
    }

    @PostMapping("/events/{id}/close")
    public String close(@PathVariable Integer id) {
        findEventOrThrow(id);
        eventService.close(id);
        return "redirect:/events/" + id;
    }

    @PostMapping("/events/{id}/reopen")
    public String reopen(@PathVariable Integer id) {
        findEventOrThrow(id);
        eventService.reopen(id);
        return "redirect:/events/" + id;
    }

    private Event findEventOrThrow(Integer id) {
        return eventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));
    }
}
