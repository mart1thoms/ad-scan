package fr.reniti.adscan.pdf;

import com.google.zxing.WriterException;
import fr.reniti.adscan.member.Member;
import fr.reniti.adscan.qrcode.QrCodeGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Renders a one-page printable badge: organization name, member details, and their access QR code. */
public final class AccessCardPdfGenerator {

    private static final int QR_PNG_SIZE = 300;
    private static final float QR_DISPLAY_SIZE = 200;
    private static final float MARGIN = 50;

    private AccessCardPdfGenerator() {
    }

    public static byte[] generate(String organizationName, Member member) throws IOException, WriterException {
        byte[] qrPng = QrCodeGenerator.png(member.accessToken(), QR_PNG_SIZE);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font textFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float y = page.getMediaBox().getHeight() - 80;

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                y = writeLine(cs, titleFont, 20, MARGIN, y, organizationName);
                y = writeLine(cs, titleFont, 14, MARGIN, y - 10, "Carte de membre");

                y -= 30;
                String fullName = member.firstName() + " " + member.lastName();
                y = writeLine(cs, textFont, 12, MARGIN, y, "Nom : " + fullName);
                y = writeLine(cs, textFont, 12, MARGIN, y, "Email : " + member.email());
                String adhesion = member.startDate().getYear()
                        + " - " + (member.endDate() != null ? member.endDate().getYear() : "en cours");
                y = writeLine(cs, textFont, 12, MARGIN, y, adhesion);

                PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, qrPng, "qr");
                cs.drawImage(qrImage, MARGIN, y - QR_DISPLAY_SIZE - 20, QR_DISPLAY_SIZE, QR_DISPLAY_SIZE);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static float writeLine(PDPageContentStream cs, PDType1Font font, float fontSize, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - (fontSize + 8);
    }
}
