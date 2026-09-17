package fr.reniti.adscan.member;

import com.google.zxing.WriterException;
import fr.reniti.adscan.qrcode.QrCodeGenerator;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class QrCodeController {

    private static final int SIZE = 300;

    private final MemberService memberService;

    public QrCodeController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping(value = "/members/{id}/qrcode.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable String id) {
        Member member = memberService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhérent introuvable"));

        try {
            byte[] png = QrCodeGenerator.png(member.accessToken(), SIZE);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(png);
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Échec de la génération du QR code", e);
        }
    }
}
