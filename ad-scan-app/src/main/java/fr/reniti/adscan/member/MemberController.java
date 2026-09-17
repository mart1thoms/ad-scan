package fr.reniti.adscan.member;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class MemberController {

    private final MemberService memberService;
    private final MemberScanHistoryRepository memberScanHistoryRepository;

    public MemberController(MemberService memberService, MemberScanHistoryRepository memberScanHistoryRepository) {
        this.memberService = memberService;
        this.memberScanHistoryRepository = memberScanHistoryRepository;
    }

    @GetMapping("/members")
    public String list(Model model) {
        model.addAttribute("members", memberService.findAll());
        return "member/list";
    }

    @GetMapping("/members/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new NewMemberForm());
        }
        model.addAttribute("formationOptions", FormationOptions.ALL);
        return "member/form";
    }

    @PostMapping("/members")
    public String create(@ModelAttribute("form") NewMemberForm form) {
        memberService.create(form.getFirstName(), form.getLastName(), form.getEmail(), form.getPhone(),
                form.getFormation(), form.getStartDate(), form.getEndDate());
        return "redirect:/members";
    }

    @GetMapping("/members/{id}")
    public String detail(@PathVariable String id, Model model) {
        Member member = findMemberOrThrow(id);
        model.addAttribute("member", member);
        model.addAttribute("tokenIssuedAt", memberService.findActiveTokenIssuedAt(id).orElse(null));
        model.addAttribute("scanHistory", memberScanHistoryRepository.findForMember(id));
        return "member/detail";
    }

    @PostMapping("/members/{id}/extend")
    public String extendMembership(@PathVariable String id, @RequestParam LocalDate newEndDate) {
        findMemberOrThrow(id);
        memberService.extendMembership(id, newEndDate);
        return "redirect:/members/" + id;
    }

    @PostMapping("/members/{id}/confirm")
    public String confirm(@PathVariable String id) {
        findMemberOrThrow(id);
        memberService.confirm(id);
        return "redirect:/members/" + id;
    }

    @PostMapping("/members/{id}/access-token/regenerate")
    public String regenerateAccessToken(@PathVariable String id) {
        findMemberOrThrow(id);
        memberService.regenerateAccessToken(id);
        return "redirect:/members/" + id;
    }

    private Member findMemberOrThrow(String id) {
        return memberService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhérent introuvable"));
    }
}
