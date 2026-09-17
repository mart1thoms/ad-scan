package fr.reniti.adscan.member;

import com.google.zxing.WriterException;
import fr.reniti.adscan.qrcode.QrCodeGenerator;
import fr.reniti.adscan.wallet.AccessRequestService;
import fr.reniti.adscan.wallet.AppleWalletService;
import fr.reniti.adscan.wallet.GoogleWalletService;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Public, unauthenticated self-registration: anyone can sign themselves up and immediately grab
 * their membership card (Google/Apple Wallet or PDF), but the resulting member starts out
 * unconfirmed ("non cotisant") until a staff member validates it from the back-office.
 */
@Controller
public class RegistrationController {

    private static final int QR_SIZE = 300;
    private static final Duration CARD_TOKEN_VALIDITY = Duration.ofHours(12);

    private final MemberService memberService;
    private final AccessRequestService accessRequestService;
    private final GoogleWalletService googleWalletService;
    private final AppleWalletService appleWalletService;

    public RegistrationController(MemberService memberService, AccessRequestService accessRequestService,
                                   GoogleWalletService googleWalletService, AppleWalletService appleWalletService) {
        this.memberService = memberService;
        this.accessRequestService = accessRequestService;
        this.googleWalletService = googleWalletService;
        this.appleWalletService = appleWalletService;
    }

    @GetMapping("/register")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegistrationForm());
        }
        model.addAttribute("formationOptions", FormationOptions.ALL);
        return "register/form";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") RegistrationForm form, Model model) {
        try {
            Member member = memberService.registerPublic(
                    form.getFirstName(), form.getLastName(), form.getEmail(), form.getPhone(), form.getFormation(),
                    form.getFiliere(), form.getAlternant());
            String token = accessRequestService.createReusableToken(member.id(), CARD_TOKEN_VALIDITY);
            return "redirect:/register/success?token=" + token;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formationOptions", FormationOptions.ALL);
            return "register/form";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Cet email est déjà utilisé pour une inscription.");
            model.addAttribute("formationOptions", FormationOptions.ALL);
            return "register/form";
        }
    }

    @GetMapping("/register/success")
    public String success(@RequestParam String token, Model model) throws WriterException, IOException {
        // Safe to "consume" here: reusable tokens (the only kind minted by /register) aren't
        // invalidated by consume(), it's only a validity + lookup check.
        Optional<Member> member = accessRequestService.consume(token);
        if (member.isEmpty()) {
            model.addAttribute("expired", true);
            return "register/success";
        }
        model.addAttribute("firstName", member.get().firstName());

        if (googleWalletService.isEnabled()) {
            String url = relayUrl(token, "google");
            model.addAttribute("googleWalletUrl", url);
            model.addAttribute("googleWalletQr", qrDataUri(url));
        }
        if (appleWalletService.isEnabled()) {
            String url = relayUrl(token, "apple");
            model.addAttribute("appleWalletUrl", url);
            model.addAttribute("appleWalletQr", qrDataUri(url));
        }
        String pdfUrl = relayUrl(token, "pdf");
        model.addAttribute("pdfUrl", pdfUrl);
        model.addAttribute("pdfQr", qrDataUri(pdfUrl));
        return "register/success";
    }

    private String relayUrl(String token, String type) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/access-request/{token}")
                .queryParam("type", type)
                .buildAndExpand(token)
                .toUriString();
    }

    private String qrDataUri(String content) throws WriterException, IOException {
        byte[] png = QrCodeGenerator.png(content, QR_SIZE);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }
}
