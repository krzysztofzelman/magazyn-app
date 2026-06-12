package com.example.magazyn.service;

import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.Product;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.repository.ProductRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class LabelService {

    private static final float MARGIN = 14f;
    private static final float A6_W = 297.64f;   // 105mm
    private static final float A6_H = 419.53f;   // 148mm
    private static final float A4_W = PDRectangle.A4.getWidth();
    private static final float A4_H = PDRectangle.A4.getHeight();

    private static final PDFont FONT_REG = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final BarcodeService barcodeService;

    public LabelService(LocationRepository locationRepository,
                        ProductRepository productRepository,
                        BarcodeService barcodeService) {
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.barcodeService = barcodeService;
    }

    public byte[] generateLocationLabel(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", locationId));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(A6_W, A6_H));
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                addLocationLabelContent(cs, doc, location, A6_W, A6_H);
            }
            doc.save(baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate location label PDF", e);
        }
        return baos.toByteArray();
    }

    public byte[] generateLocationLabels(List<Long> locationIds) {
        List<Location> locations = locationRepository.findAllById(locationIds);
        if (locations.isEmpty()) {
            throw new ResourceNotFoundException("No locations found for given IDs");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            float cellW = (A4_W - 2 * MARGIN) / 2;
            float cellH = (A4_H - 2 * MARGIN) / 2;

            for (int i = 0; i < locations.size(); i++) {
                if (i % 4 == 0) {
                    doc.addPage(new PDPage(PDRectangle.A4));
                }
                Location location = locations.get(i);
                int posOnPage = i % 4;
                // rows: pos 0,1 = top row; pos 2,3 = bottom row
                // cols: even = left, odd = right
                int row = posOnPage / 2;
                int col = posOnPage % 2;
                float cellX = MARGIN + col * cellW;
                float cellY = MARGIN + (1 - row) * cellH;

                PDPage page = doc.getPage(doc.getNumberOfPages() - 1);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    // Save and clip to cell
                    cs.saveGraphicsState();
                    // Draw cell border (light gray) — optional debugging aid
                    cs.setStrokingColor(0.85f);
                    cs.addRect(cellX, cellY, cellW, cellH);
                    cs.stroke();
                    // Clip to cell
                    cs.addRect(cellX, cellY, cellW, cellH);
                    cs.clip();

                    // Draw content inside cell
                    float centerX = cellX + cellW / 2;
                    float topY = cellY + cellH - 6f;
                    float y = topY;

                    y = drawCenteredText(cs, FONT_BOLD, 11, "LOKALIZACJA", centerX, y, cellW - 8);
                    y = drawCenteredText(cs, FONT_BOLD, 10, location.getName(), centerX, y, cellW - 8);
                    y = drawCenteredText(cs, FONT_REG, 8, "Kod: " + location.getCode(), centerX, y, cellW - 8);

                    if (location.getZone() != null && !location.getZone().isBlank()) {
                        y = drawCenteredText(cs, FONT_REG, 8, "Strefa: " + location.getZone(), centerX, y, cellW - 8);
                    }
                    y = drawCenteredText(cs, FONT_REG, 8, "Typ: " + location.getType().name(), centerX, y, cellW - 8);

                    float imgY = y - 6;
                    if (location.getBarcode() != null) {
                        byte[] barcodePng = barcodeService.generateBarcodeImage(location.getBarcode());
                        imgY = drawImageCentered(cs, doc, barcodePng, centerX, imgY, 140);
                    }
                    if (location.getQrData() != null) {
                        byte[] qrPng = barcodeService.generateQrImage(location.getQrData());
                        imgY = drawImageCentered(cs, doc, qrPng, centerX, imgY, 55);
                    }

                    cs.restoreGraphicsState();
                }
            }
            doc.save(baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate multi-location label PDF", e);
        }
        return baos.toByteArray();
    }

    public byte[] generateProductLabel(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(A6_W, A6_H));
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                addProductLabelContent(cs, doc, product, A6_W, A6_H);
            }
            doc.save(baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate product label PDF", e);
        }
        return baos.toByteArray();
    }

    // ============= ZPL label generation =============

    private static final int ZPL_DPI = 203;             // 203 dpi
    private static final int ZPL_W = 800;                // ~100mm
    private static final int ZPL_H = 600;                // ~75mm
    private static final int ZPL_MARGIN = 20;

    public String generateProductZpl(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        String barcodeText = product.getBarcode() != null ? product.getBarcode() : product.getSku();
        String name = truncateZpl(product.getName(), 30);
        String sku = product.getSku();

        StringBuilder zpl = new StringBuilder();
        zpl.append("^XA");                                         // start label
        zpl.append("^LH0,0");                                      // label home
        zpl.append("^LL").append(ZPL_H);                           // label length

        // Font: A=0 (CG Triumvirate Bold Condensed), default size 30
        zpl.append("^CF0,30");
        // Header
        zpl.append("^FO").append(ZPL_MARGIN).append(",").append(ZPL_MARGIN);
        zpl.append("^FD").append("PRODUKT").append("^FS");

        // Product name — larger font
        zpl.append("^CF0,40");
        zpl.append("^FO").append(ZPL_MARGIN).append(",55");
        zpl.append("^FD").append(name).append("^FS");

        // SKU
        zpl.append("^CF0,24");
        zpl.append("^FO").append(ZPL_MARGIN).append(",110");
        zpl.append("^FDSKU: ").append(sku).append("^FS");

        // Barcode (Code 128)
        zpl.append("^BY2,3,120");                                  // module width=2, ratio=3, height=120
        zpl.append("^FO").append(ZPL_MARGIN).append(",160");
        zpl.append("^BCN,120,Y,N,N");
        zpl.append("^FD").append(barcodeText).append("^FS");

        // Barcode text below
        zpl.append("^CF0,22");
        zpl.append("^FO").append(ZPL_MARGIN).append(",290");
        zpl.append("^FD").append(barcodeText).append("^FS");

        zpl.append("^XZ");                                         // end label
        return zpl.toString();
    }

    public String generateLocationZpl(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", locationId));

        String barcodeText = location.getBarcode() != null ? location.getBarcode() : location.getCode();
        String name = truncateZpl(location.getName(), 30);
        String code = location.getCode();

        StringBuilder zpl = new StringBuilder();
        zpl.append("^XA");
        zpl.append("^LH0,0");
        zpl.append("^LL").append(ZPL_H);

        // Header
        zpl.append("^CF0,30");
        zpl.append("^FO").append(ZPL_MARGIN).append(",").append(ZPL_MARGIN);
        zpl.append("^FD").append("LOKALIZACJA").append("^FS");

        // Location name — larger
        zpl.append("^CF0,40");
        zpl.append("^FO").append(ZPL_MARGIN).append(",55");
        zpl.append("^FD").append(name).append("^FS");

        // Code
        zpl.append("^CF0,24");
        zpl.append("^FO").append(ZPL_MARGIN).append(",110");
        zpl.append("^FDKod: ").append(code).append("^FS");

        // Type
        zpl.append("^FO").append(ZPL_MARGIN).append(",140");
        zpl.append("^FDTyp: ").append(location.getType().name()).append("^FS");

        // Zone
        if (location.getZone() != null && !location.getZone().isBlank()) {
            zpl.append("^FO").append(ZPL_MARGIN).append(",170");
            zpl.append("^FDStrefa: ").append(location.getZone()).append("^FS");
        }

        // Barcode (Code 128)
        zpl.append("^BY2,3,120");
        zpl.append("^FO").append(ZPL_MARGIN).append(",210");
        zpl.append("^BCN,120,Y,N,N");
        zpl.append("^FD").append(barcodeText).append("^FS");

        // Barcode text below
        zpl.append("^CF0,22");
        zpl.append("^FO").append(ZPL_MARGIN).append(",340");
        zpl.append("^FD").append(barcodeText).append("^FS");

        zpl.append("^XZ");
        return zpl.toString();
    }

    private static String truncateZpl(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    // ---- A6 label helpers ----

    private void addLocationLabelContent(PDPageContentStream cs, PDDocument doc,
                                          Location location, float pageW, float pageH) throws IOException {
        float centerX = pageW / 2;
        float y = pageH - MARGIN;

        y = drawCenteredText(cs, FONT_BOLD, 16, "LOKALIZACJA", centerX, y, pageW - 2 * MARGIN);
        y = drawCenteredText(cs, FONT_BOLD, 14, location.getName(), centerX, y, pageW - 2 * MARGIN);
        y = drawCenteredText(cs, FONT_REG, 10, "Kod: " + location.getCode(), centerX, y, pageW - 2 * MARGIN);

        if (location.getZone() != null && !location.getZone().isBlank()) {
            y = drawCenteredText(cs, FONT_REG, 10, "Strefa: " + location.getZone(), centerX, y, pageW - 2 * MARGIN);
        }
        y = drawCenteredText(cs, FONT_REG, 10, "Typ: " + location.getType().name(), centerX, y, pageW - 2 * MARGIN);

        y -= 8;
        if (location.getBarcode() != null) {
            byte[] barcodePng = barcodeService.generateBarcodeImage(location.getBarcode());
            y = drawImageCentered(cs, doc, barcodePng, centerX, y, 200);
        }
        if (location.getQrData() != null) {
            byte[] qrPng = barcodeService.generateQrImage(location.getQrData());
            drawImageCentered(cs, doc, qrPng, centerX, y, 80);
        }
    }

    private void addProductLabelContent(PDPageContentStream cs, PDDocument doc,
                                         Product product, float pageW, float pageH) throws IOException {
        float centerX = pageW / 2;
        float y = pageH - MARGIN;

        y = drawCenteredText(cs, FONT_BOLD, 16, "PRODUKT", centerX, y, pageW - 2 * MARGIN);
        y = drawCenteredText(cs, FONT_BOLD, 14, product.getName(), centerX, y, pageW - 2 * MARGIN);
        y = drawCenteredText(cs, FONT_REG, 10, "SKU: " + product.getSku(), centerX, y, pageW - 2 * MARGIN);

        String barcodeText = product.getBarcode() != null
                ? product.getBarcode()
                : "PROD-" + product.getId();

        y -= 8;
        byte[] barcodePng;
        if (product.getBarcode() != null) {
            barcodePng = barcodeService.generateBarcodeImage(product.getBarcode());
        } else {
            barcodePng = barcodeService.generateBarcodeImage(barcodeText);
        }
        drawImageCentered(cs, doc, barcodePng, centerX, y, 200);
    }

    // ---- PDFBox drawing utilities ----

    private float drawCenteredText(PDPageContentStream cs, PDFont font, int fontSize,
                                    String text, float centerX, float y, float maxWidth) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        if (textWidth > maxWidth) {
            float scale = maxWidth / textWidth;
            cs.setFont(font, fontSize * scale);
            textWidth = maxWidth;
        } else {
            cs.setFont(font, fontSize);
        }
        float x = centerX - textWidth / 2;
        cs.beginText();
        cs.newLineAtOffset(x, y - fontSize);
        cs.showText(text);
        cs.endText();
        return y - fontSize - 4f;
    }

    private float drawImageCentered(PDPageContentStream cs, PDDocument doc, byte[] imageBytes,
                                     float centerX, float y, float maxWidth) throws IOException {
        PDImageXObject img = PDImageXObject.createFromByteArray(doc, imageBytes, null);
        float imgW = img.getWidth();
        float imgH = img.getHeight();
        float scale = Math.min(maxWidth / imgW, 1f);
        float drawW = imgW * scale;
        float drawH = imgH * scale;
        float x = centerX - drawW / 2;
        cs.drawImage(img, x, y - drawH, drawW, drawH);
        return y - drawH - 6f;
    }
}
