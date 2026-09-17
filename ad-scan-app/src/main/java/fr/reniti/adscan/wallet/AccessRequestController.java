package fr.reniti.adscan.wallet;

import com.google.zxing.WriterException;
import fr.reniti.adscan.config.AppProperties;
import fr.reniti.adscan.member.Member;
import fr.reniti.adscan.pdf.AccessCardPdfGenerator;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unauthenticated endpoint: resolves a short-lived {@link AccessRequestService} token into the
 * actual wallet link / pass / PDF. Meant to be hit directly by a member's own phone (via the QR
 * shown on the staff-facing access-card page), which has no login session.
 */
@RestController
public class AccessRequestController {

    private static final MediaType PKPASS_MEDIA_TYPE = MediaType.parseMediaType("application/vnd.apple.pkpass");

    private final AccessRequestService accessRequestService;
    private final GoogleWalletService googleWalletService;
    private final AppleWalletService appleWalletService;
    private final AppProperties appProperties;

    public AccessRequestController(AccessRequestService accessRequestService, GoogleWalletService googleWalletService,
                                    AppleWalletService appleWalletService, AppProperties appProperties) {
        this.accessRequestService = accessRequestService;
        this.googleWalletService = googleWalletService;
        this.appleWalletService = appleWalletService;
        this.appProperties = appProperties;
    }

    @GetMapping("/access-request/{token}")
    public ResponseEntity<byte[]> consume(@PathVariable String token, @RequestParam String type) {
        Member member = accessRequestService.consume(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "Ce lien a expiré ou a déjà été utilisé."));

        try {
            return switch (type) {
                case "google" -> {
                    if (!googleWalletService.isEnabled()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Google Wallet n'est pas configuré.");
                    }
                    yield ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(googleWalletService.buildSaveUrl(member)))
                            .build();
                }
                case "apple" -> {
                    if (!appleWalletService.isEnabled()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Apple Wallet n'est pas configuré.");
                    }
                    yield ResponseEntity.ok()
                            .contentType(PKPASS_MEDIA_TYPE)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"membre-" + member.id() + ".pkpass\"")
                            .body(appleWalletService.buildPkPass(member));
                }
                case "pdf" -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"carte-membre-" + member.id() + ".pdf\"")
                        .body(AccessCardPdfGenerator.generate(appProperties.name(), member));
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de demande invalide.");
            };
        } catch (IOException | WriterException e) {
            throw new IllegalStateException("Échec de la génération de la carte d'accès", e);
        }
    }
}
