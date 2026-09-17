package fr.reniti.adscan.wallet;

import de.brendamour.jpasskit.PKBarcode;
import de.brendamour.jpasskit.PKField;
import de.brendamour.jpasskit.PKPass;
import de.brendamour.jpasskit.enums.PKBarcodeFormat;
import de.brendamour.jpasskit.enums.PKPassType;
import de.brendamour.jpasskit.passes.PKGenericPass;
import de.brendamour.jpasskit.signing.PKInMemorySigningUtil;
import de.brendamour.jpasskit.signing.PKPassTemplateInMemory;
import de.brendamour.jpasskit.signing.PKSigningException;
import de.brendamour.jpasskit.signing.PKSigningInformation;
import de.brendamour.jpasskit.signing.PKSigningInformationUtil;
import fr.reniti.adscan.config.AppProperties;
import fr.reniti.adscan.member.Member;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

/**
 * Builds signed .pkpass files for members' membership card (Apple Wallet "store card" pass style —
 * chosen over "generic" specifically because it's the style that renders a strip/banner image),
 * using jpasskit. Requires a Pass Type ID certificate exported as PKCS#12 (.p12) and Apple's WWDR
 * intermediate certificate, both created once via the Apple Developer portal (see data/config.yml.example).
 */
@Service
public class AppleWalletService {

    private static final Color DEFAULT_COLOR = new Color(0x2b, 0x3a, 0x55);

    // Apple's recommended @1x point sizes; @2x/@3x are simply 2x/3x these (developer.apple.com/documentation/walletpasses).
    private static final int LOGO_WIDTH = 160;
    private static final int LOGO_HEIGHT = 50;
    private static final int STRIP_WIDTH = 375;
    private static final int STRIP_HEIGHT = 123;

    private static final String ICON_PATH = "branding/icon.png";
    private static final String LOGO_PATH = "branding/logo.png";
    private static final String BANNER_PATH = "branding/banner.png";

    private final AppProperties.AppleWallet config;
    private final AppProperties.WalletBranding branding;

    private volatile PKSigningInformation signingInformation;

    public AppleWalletService(AppProperties appProperties) {
        this.config = appProperties.appleWallet();
        this.branding = appProperties.wallet();
    }

    public boolean isEnabled() {
        return config != null
                && config.passTypeIdentifier() != null && !config.passTypeIdentifier().isBlank()
                && config.teamIdentifier() != null && !config.teamIdentifier().isBlank();
    }

    public byte[] buildPkPass(Member member) {
        if (!isEnabled()) {
            throw new IllegalStateException(
                    "Apple Wallet n'est pas activé : renseignez app.apple-wallet.pass-type-identifier et team-identifier.");
        }
        loadSigningInformationIfNeeded();

        String fullName = member.firstName() + " " + member.lastName();
        String adhesion = member.startDate().getYear() + " → " + (member.endDate() != null ? member.endDate().getYear() : "en cours");
        Color backgroundColor = parseColor(branding.backgroundColor());

        PKPass pass = PKPass.builder()
                .formatVersion(1)
                .passTypeIdentifier(config.passTypeIdentifier())
                .teamIdentifier(config.teamIdentifier())
                .organizationName(branding.name())
                .serialNumber("member-" + member.id())
                .description(branding.name() + " - Carte d'adhérent")
                .backgroundColor(toRgbString(backgroundColor))
                .foregroundColor("rgb(255,255,255)")
                .pass(PKGenericPass.builder()
                        .passType(PKPassType.PKStoreCard)
                        .primaryField(PKField.builder().key("name").label("Adhérent").value(fullName).build())
                        .secondaryField(PKField.builder().key("email").label("Email").value(member.email()).build())
                        .auxiliaryField(PKField.builder().key("adhesion").label("Adhésion").value(adhesion).build()))
                .barcodeBuilder(PKBarcode.builder()
                        .format(PKBarcodeFormat.PKBarcodeFormatQR)
                        .message(member.accessToken())
                        .messageEncoding(StandardCharsets.UTF_8))
                .build();

        PKPassTemplateInMemory template = new PKPassTemplateInMemory();
        try {
            template.addFile(PKPassTemplateInMemory.PK_ICON, new ByteArrayInputStream(iconBytes(29, backgroundColor)));
            template.addFile(PKPassTemplateInMemory.PK_ICON_RETINA, new ByteArrayInputStream(iconBytes(58, backgroundColor)));
            template.addFile(PKPassTemplateInMemory.PK_ICON_RETINAHD, new ByteArrayInputStream(iconBytes(87, backgroundColor)));

            BufferedImage logo = loadImageOrNull(LOGO_PATH);
            if (logo != null) {
                template.addFile(PKPassTemplateInMemory.PK_LOGO, new ByteArrayInputStream(fitImage(logo, LOGO_WIDTH, LOGO_HEIGHT)));
                template.addFile(PKPassTemplateInMemory.PK_LOGO_RETINA, new ByteArrayInputStream(fitImage(logo, LOGO_WIDTH * 2, LOGO_HEIGHT * 2)));
                template.addFile(PKPassTemplateInMemory.PK_LOGO_RETINAHD, new ByteArrayInputStream(fitImage(logo, LOGO_WIDTH * 3, LOGO_HEIGHT * 3)));
            }

            BufferedImage banner = loadImageOrNull(BANNER_PATH);
            if (banner != null) {
                template.addFile(PKPassTemplateInMemory.PK_STRIP, new ByteArrayInputStream(fitImage(banner, STRIP_WIDTH, STRIP_HEIGHT)));
                template.addFile(PKPassTemplateInMemory.PK_STRIP_RETINA, new ByteArrayInputStream(fitImage(banner, STRIP_WIDTH * 2, STRIP_HEIGHT * 2)));
                template.addFile(PKPassTemplateInMemory.PK_STRIP_RETINAHD, new ByteArrayInputStream(fitImage(banner, STRIP_WIDTH * 3, STRIP_HEIGHT * 3)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de préparer les images du pass Apple Wallet", e);
        }

        try {
            return new PKInMemorySigningUtil().createSignedAndZippedPkPassArchive(pass, template, signingInformation);
        } catch (PKSigningException e) {
            throw new IllegalStateException("Échec de la signature du pass Apple Wallet", e);
        }
    }

    private byte[] iconBytes(int size, Color fallbackColor) throws IOException {
        BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        BufferedImage source = loadImageOrNull(ICON_PATH);
        if (source != null) {
            g.drawImage(source, 0, 0, size, size, null);
        } else {
            g.setColor(fallbackColor);
            g.fillRect(0, 0, size, size);
        }
        g.dispose();
        return toPngBytes(target);
    }

    /** Scales the source image to fit within boxWidth x boxHeight, preserving aspect ratio, centered on a transparent canvas. */
    private byte[] fitImage(BufferedImage source, int boxWidth, int boxHeight) throws IOException {
        BufferedImage target = new BufferedImage(boxWidth, boxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = Math.min((double) boxWidth / source.getWidth(), (double) boxHeight / source.getHeight());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int x = (boxWidth - width) / 2;
        int y = (boxHeight - height) / 2;
        g.drawImage(source, x, y, width, height, null);
        g.dispose();
        return toPngBytes(target);
    }

    private static byte[] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static BufferedImage loadImageOrNull(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Path file = Path.of(path);
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return ImageIO.read(file.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) {
            return DEFAULT_COLOR;
        }
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            return DEFAULT_COLOR;
        }
    }

    private static String toRgbString(Color color) {
        return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
    }

    private synchronized void loadSigningInformationIfNeeded() {
        if (signingInformation != null) {
            return;
        }
        Path p12Path = Path.of(config.p12Path());
        Path wwdrPath = Path.of(config.wwdrCertPath());
        if (!Files.exists(p12Path)) {
            throw new IllegalStateException("Fichier .p12 Apple Wallet introuvable : " + p12Path.toAbsolutePath());
        }
        if (!Files.exists(wwdrPath)) {
            throw new IllegalStateException("Certificat WWDR Apple introuvable : " + wwdrPath.toAbsolutePath());
        }
        try (InputStream p12In = Files.newInputStream(p12Path);
             InputStream wwdrIn = Files.newInputStream(wwdrPath)) {
            signingInformation = new PKSigningInformationUtil()
                    .loadSigningInformationFromPKCS12AndIntermediateCertificate(p12In, config.p12Password(), wwdrIn);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de charger les certificats Apple Wallet", e);
        } catch (CertificateException e) {
            throw new IllegalStateException("Certificat Apple Wallet invalide", e);
        }
    }
}
