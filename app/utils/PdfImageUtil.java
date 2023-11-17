package utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import play.libs.Files.TemporaryFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfImageUtil {

    private static final String imageFormat = "png";
    private static final int renderDpi = 300;

    public static boolean isContentType(TemporaryFile file, String type) {
        String contentType = null;
        try {
            contentType = new Tika().detect(file.path().toFile());
        }
        catch (IOException exc) {
            return false;
        }
        return contentType.equals(type);
    }

    public static boolean isPdf(TemporaryFile file) {
        return isContentType(file, "application/pdf");
    }

    public static boolean isZip(TemporaryFile file) {
        return isContentType(file, "application/zip");
    }

    public static boolean isText(TemporaryFile file) {
        return isContentType(file, "text/plain");
    }

    public static List<BufferedImage> convertPdfToImages(TemporaryFile pdf) {
        List<BufferedImage> result = new ArrayList<>();
        try {
            PDDocument document = PDDocument.load(pdf.path().toFile());
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, renderDpi);
                result.add(bim);
            }
        }
        catch (IOException exc) {
            return result;
        }
        return result;
    }


    public static byte[] convertImageToBytes(BufferedImage image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, imageFormat, baos);
        }
        catch (IOException exc) {
            return baos.toByteArray();
        }
        return baos.toByteArray();
    }

    public static String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}
