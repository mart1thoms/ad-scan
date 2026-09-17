package fr.reniti.adscan.wallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.reniti.adscan.config.AppProperties;
import fr.reniti.adscan.member.Member;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds "Save to Google Wallet" links for members' membership card, using the Generic pass type.
 * Both the class (id = issuerId.classSuffix) and the per-member object are embedded directly in the
 * signed JWT, so nothing needs to be pre-created via the Google Pay &amp; Wallet Business Console —
 * for a Generic pass, virtually all visible content lives on the object, so the embedded class is
 * intentionally minimal (just its id).
 */
@Service
public class GoogleWalletService {

    private static final Logger log = LoggerFactory.getLogger(GoogleWalletService.class);
    private static final String SAVE_URL_PREFIX = "https://pay.google.com/gp/v/save/";

    private static final String LOGO_FILENAME = "branding/logo-google.png";
    private static final String BANNER_FILENAME = "branding/banner.png";

    private final AppProperties.GoogleWallet config;
    private final AppProperties.WalletBranding branding;
    private final String publicBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile PrivateKey privateKey;
    private volatile String serviceAccountEmail;

    public GoogleWalletService(AppProperties appProperties) {
        this.config = appProperties.googleWallet();
        this.branding = appProperties.wallet();
        this.publicBaseUrl = appProperties.publicBaseUrl();
    }

    public boolean isEnabled() {
        return config != null && config.issuerId() != null && !config.issuerId().isBlank();
    }

    public String buildSaveUrl(Member member) {
        if (!isEnabled()) {
            throw new IllegalStateException("Google Wallet n'est pas activé : renseignez app.google-wallet.issuer-id.");
        }
        loadCredentialsIfNeeded();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("genericClasses", List.of(Map.of("id", classId())));
        payload.put("genericObjects", List.of(buildGenericObject(member)));
        logPayload(payload);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", serviceAccountEmail);
        claims.put("aud", "google");
        claims.put("typ", "savetowallet");
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("payload", payload);

        String jwt = Jwts.builder()
                .claims(claims)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        return SAVE_URL_PREFIX + jwt;
    }

    private String classId() {
        return config.issuerId() + "." + config.classSuffix();
    }

    private Map<String, Object> buildGenericObject(Member member) {
        String fullName = member.firstName() + " " + member.lastName();

        Map<String, Object> object = new LinkedHashMap<>();
        object.put("id", config.issuerId() + ".member_" + member.id());
        object.put("classId", classId());
        object.put("state", "ACTIVE");
        object.put("cardTitle", localizedString(branding.name()));
        object.put("header", localizedString(fullName));
        object.put("subheader", localizedString("Carte d'adhérent"));
        object.put("barcode", Map.of("type", "QR_CODE", "value", member.accessToken()));
        object.put("textModulesData", List.of(
                textModule("email", "Email", member.email()),
                textModule("adhesion", "Adhésion",
                        member.startDate().getYear() + " → " + (member.endDate() != null ? member.endDate().getYear() : "en cours"))
        ));
        if (branding.backgroundColor() != null && !branding.backgroundColor().isBlank()) {
            object.put("hexBackgroundColor", branding.backgroundColor());
        }
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            if (Files.exists(Path.of(LOGO_FILENAME))) {
                object.put("logo", Map.of("sourceUri", Map.of("uri", publicBaseUrl + "/" + LOGO_FILENAME)));
            }
            if (Files.exists(Path.of(BANNER_FILENAME))) {
                object.put("heroImage", Map.of("sourceUri", Map.of("uri", publicBaseUrl + "/" + BANNER_FILENAME)));
            }
        }
        return object;
    }

    private void logPayload(Map<String, Object> payload) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            log.info("Google Wallet JWT payload brut (à tester dans la console Google) :\n{}", json);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser le payload Google Wallet pour le debug", e);
        }
    }

    private static Map<String, Object> localizedString(String value) {
        return Map.of("defaultValue", Map.of("language", "fr", "value", value));
    }

    private static Map<String, Object> textModule(String id, String header, String body) {
        Map<String, Object> module = new LinkedHashMap<>();
        module.put("id", id);
        module.put("header", header);
        module.put("body", body);
        return module;
    }

    private synchronized void loadCredentialsIfNeeded() {
        if (privateKey != null) {
            return;
        }
        Path keyPath = Path.of(config.serviceAccountKeyPath());
        if (!Files.exists(keyPath)) {
            throw new IllegalStateException(
                    "Fichier de clé de service Google Wallet introuvable : " + keyPath.toAbsolutePath());
        }
        try {
            JsonNode json = objectMapper.readTree(keyPath.toFile());
            String email = json.path("client_email").asText();
            String pem = json.path("private_key").asText();
            if (email.isEmpty() || pem.isEmpty()) {
                throw new IllegalStateException(
                        "Le fichier de clé de service Google Wallet ne contient pas client_email/private_key.");
            }
            this.privateKey = parsePrivateKey(pem);
            this.serviceAccountEmail = email;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier de clé de service Google Wallet", e);
        }
    }

    private static PrivateKey parsePrivateKey(String pem) {
        String sanitized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(sanitized);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Clé privée Google Wallet invalide", e);
        }
    }
}
