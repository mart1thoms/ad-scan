package fr.reniti.adscan.wallet;

import com.google.zxing.WriterException;
import fr.reniti.adscan.config.AppProperties;
import fr.reniti.adscan.member.Member;
import fr.reniti.adscan.member.MemberService;
import fr.reniti.adscan.pdf.AccessCardPdfGenerator;
import fr.reniti.adscan.qrcode.QrCodeGenerator;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class AccessCardController {

    private static final int QR_SIZE = 300;

    private final MemberService memberService;
    private final GoogleWalletService googleWalletService;
    private final AppleWalletService appleWalletService;
    private final AccessRequestService accessRequestService;
    private final AppProperties appProperties;

    public AccessCardController(MemberService memberService, GoogleWalletService googleWalletService,
                                 AppleWalletService appleWalletService, AccessRequestService accessRequestService,
                                 AppProperties appProperties) {
        this.memberService = memberService;
        this.googleWalletService = googleWalletService;
        this.appleWalletService = appleWalletService;
        this.accessRequestService = accessRequestService;
        this.appProperties = appProperties;
    }

    @GetMapping("/members/{id}/access-card")
    public String accessCard(@PathVariable String id, Model model) {
        model.addAttribute("member", findMemberOrThrow(id));
        return "member/access-card";
    }

    // Called from the "Partager les accès" button: only now is a fresh token minted and the 3
    // relay QR codes generated. Nothing is created just by opening the member's page.
    @PostMapping("/members/{id}/access-card/share")
    @ResponseBody
    public Map<String, String> shareAccess(@PathVariable String id) {
        findMemberOrThrow(id);
        String token = accessRequestService.createToken(id);

        Map<String, String> result = new LinkedHashMap<>();
        try {
            if (googleWalletService.isEnabled()) {
                String url = relayUrl(token, "google");
                result.put("googleWalletUrl", url);
                result.put("googleWalletQr", qrDataUri(url));
            }
            if (appleWalletService.isEnabled()) {
                String url = relayUrl(token, "apple");
                result.put("appleWalletUrl", url);
                result.put("appleWalletQr", qrDataUri(url));
            }
            String pdfUrl = relayUrl(token, "pdf");
            result.put("pdfRelayUrl", pdfUrl);
            result.put("pdfRelayQr", qrDataUri(pdfUrl));
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Échec de la génération des QR codes de la carte d'accès", e);
        }
        return result;
    }

    @GetMapping("/members/{id}/access-card.pdf")
    public ResponseEntity<byte[]> accessCardPdf(@PathVariable String id) {
        Member member = findMemberOrThrow(id);
        try {
            byte[] pdf = AccessCardPdfGenerator.generate(appProperties.name(), member);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"carte-membre-" + member.id() + ".pdf\"")
                    .body(pdf);
        } catch (IOException | WriterException e) {
            throw new IllegalStateException("Échec de la génération du PDF de la carte d'accès", e);
        }
    }

    private String relayUrl(String token, String type) {
        // Absolute URL matching whatever host the browser used to load this page, since the link/QR
        // is meant to be opened or scanned by the member's own phone, on the same network.
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

    private Member findMemberOrThrow(String id) {
        return memberService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhérent introuvable"));
    }
}
