package fr.reniti.adscan.pdf;

import com.google.zxing.WriterException;
import fr.reniti.adscan.config.AppProperties;
import fr.reniti.adscan.member.Member;
import fr.reniti.adscan.qrcode.QrCodeGenerator;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Renders a one-page printable badge: branded header, member details, and their access QR code. */
public final class AccessCardPdfGenerator {

    private static final Color DEFAULT_BRAND_COLOR = new Color(0x2b, 0x3a, 0x55);
    private static final Color INK = new Color(0x1f, 0x29, 0x37);
    private static final Color MUTED = new Color(0x6b, 0x72, 0x80);
    private static final Color RULE = new Color(0xe2, 0xe5, 0xea);

    private static final String LOGO_PATH = "branding/logo.png";
    private static final int QR_PNG_SIZE = 320;
    private static final float QR_DISPLAY_SIZE = 150;
    private static final float MARGIN = 42;
    private static final float HEADER_HEIGHT = 108;
    private static final float CORNER_RADIUS = 10;

    private AccessCardPdfGenerator() {
    }

    public static byte[] generate(AppProperties appProperties, Member member) throws IOException, WriterException {
        byte[] qrPng = QrCodeGenerator.png(member.accessToken(), QR_PNG_SIZE);
        Color brandColor = parseColor(appProperties.wallet().backgroundColor());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Header band
                cs.setNonStrokingColor(brandColor);
                cs.addRect(0, pageHeight - HEADER_HEIGHT, pageWidth, HEADER_HEIGHT);
                cs.fill();

                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, bold, 22, MARGIN, pageHeight - 52, "Carte d'adhérent");
                drawText(cs, regular, 11, MARGIN, pageHeight - 74, appProperties.name().toUpperCase());

                PDImageXObject logo = loadLogo(document);
                if (logo != null) {
                    float logoHeight = 48;
                    float logoWidth = logoHeight * logo.getWidth() / (float) logo.getHeight();
                    cs.drawImage(logo, pageWidth - MARGIN - logoWidth, pageHeight - HEADER_HEIGHT / 2 - logoHeight / 2,
                            logoWidth, logoHeight);
                }

                // Member name
                float nameBaseline = pageHeight - HEADER_HEIGHT - 48;
                cs.setNonStrokingColor(INK);
                drawText(cs, bold, 20, MARGIN, nameBaseline, member.fullName());

                cs.setStrokingColor(RULE);
                cs.setLineWidth(1);
                cs.moveTo(MARGIN, nameBaseline - 16);
                cs.lineTo(pageWidth - MARGIN, nameBaseline - 16);
                cs.stroke();

                // Detail rows (left column)
                float detailWidth = pageWidth - MARGIN * 3 - QR_DISPLAY_SIZE;
                float rowY = nameBaseline - 44;
                rowY = drawDetailRow(cs, bold, regular, MARGIN, rowY, "EMAIL", member.email());
                rowY = drawDetailRow(cs, bold, regular, MARGIN, rowY, "PROVENANCE", member.provenance());
                drawDetailRow(cs, bold, regular, MARGIN, rowY, "ADHÉSION", adhesionRange(member));

                // QR box (right column)
                float qrBoxSize = QR_DISPLAY_SIZE + 24;
                float qrBoxX = pageWidth - MARGIN - qrBoxSize;
                float qrBoxY = nameBaseline - 20 - qrBoxSize;
                cs.setStrokingColor(RULE);
                cs.setLineWidth(1);
                strokeRoundedRect(cs, qrBoxX, qrBoxY, qrBoxSize, qrBoxSize, CORNER_RADIUS);

                PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, qrPng, "qr");
                cs.drawImage(qrImage, qrBoxX + 12, qrBoxY + 12, QR_DISPLAY_SIZE, QR_DISPLAY_SIZE);

                cs.setNonStrokingColor(MUTED);
                drawTextCentered(cs, regular, 8, qrBoxX + qrBoxSize / 2, qrBoxY - 14, "Présenter à l'entrée");

                // Footer
                cs.setStrokingColor(RULE);
                cs.moveTo(MARGIN, MARGIN + 20);
                cs.lineTo(pageWidth - MARGIN, MARGIN + 20);
                cs.stroke();

                cs.setNonStrokingColor(MUTED);
                String footer = "Carte générée le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " · " + appProperties.name() + " · à usage personnel, ne pas partager";
                drawText(cs, regular, 8, MARGIN, MARGIN, footer);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static String adhesionRange(Member member) {
        return member.startDate().getYear() + " - " + (member.endDate() != null ? member.endDate().getYear() : "en cours");
    }

    private static float drawDetailRow(PDPageContentStream cs, PDType1Font labelFont, PDType1Font valueFont,
                                        float x, float y, String label, String value) throws IOException {
        cs.setNonStrokingColor(MUTED);
        drawText(cs, labelFont, 9, x, y, label);
        cs.setNonStrokingColor(INK);
        drawText(cs, valueFont, 12, x, y - 16, value);
        return y - 42;
    }

    private static void drawText(PDPageContentStream cs, PDType1Font font, float fontSize, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static void drawTextCentered(PDPageContentStream cs, PDType1Font font, float fontSize, float centerX, float y,
                                          String text) throws IOException {
        float width = font.getStringWidth(text) / 1000 * fontSize;
        drawText(cs, font, fontSize, centerX - width / 2, y, text);
    }

    private static void strokeRoundedRect(PDPageContentStream cs, float x, float y, float width, float height, float r)
            throws IOException {
        float k = r * 0.5523f;
        cs.moveTo(x + r, y);
        cs.lineTo(x + width - r, y);
        cs.curveTo(x + width - r + k, y, x + width, y + r - k, x + width, y + r);
        cs.lineTo(x + width, y + height - r);
        cs.curveTo(x + width, y + height - r + k, x + width - r + k, y + height, x + width - r, y + height);
        cs.lineTo(x + r, y + height);
        cs.curveTo(x + r - k, y + height, x, y + height - r + k, x, y + height - r);
        cs.lineTo(x, y + r);
        cs.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        cs.closePath();
        cs.stroke();
    }

    private static PDImageXObject loadLogo(PDDocument document) throws IOException {
        Path file = Path.of(LOGO_PATH);
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return LosslessFactory.createFromImage(document, ImageIO.read(file.toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) {
            return DEFAULT_BRAND_COLOR;
        }
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            return DEFAULT_BRAND_COLOR;
        }
    }
}
