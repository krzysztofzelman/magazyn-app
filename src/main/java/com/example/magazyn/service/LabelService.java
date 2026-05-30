package com.example.magazyn.service;

import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.Product;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.repository.ProductRepository;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class LabelService {

    private static final float MARGIN = 14f;

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
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A6);
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

        addLocationLabelContent(document, location);

        document.close();
        return baos.toByteArray();
    }

    public byte[] generateLocationLabels(List<Long> locationIds) {
        List<Location> locations = locationRepository.findAllById(locationIds);
        if (locations.isEmpty()) {
            throw new ResourceNotFoundException("No locations found for given IDs");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        table.setWidth(UnitValue.createPercentValue(100));

        boolean hasCells = false;
        int count = 0;
        for (Location location : locations) {
            if (count > 0 && count % 4 == 0) {
                document.add(table);
                table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
                table.setWidth(UnitValue.createPercentValue(100));
                hasCells = false;
            }

            Cell cell = new Cell();
            cell.setPadding(8);
            cell.setBorder(Border.NO_BORDER);
            addLocationCellContent(cell, location);
            table.addCell(cell);
            hasCells = true;
            count++;
        }

        if (hasCells) {
            document.add(table);
        }

        document.close();
        return baos.toByteArray();
    }

    public byte[] generateProductLabel(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A6);
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

        addProductLabelContent(document, product);

        document.close();
        return baos.toByteArray();
    }

    private void addLocationLabelContent(Document document, Location location) {
        document.add(new Paragraph("LOKALIZACJA")
                .setBold()
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(location.getName())
                .setBold()
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Kod: " + location.getCode())
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER));

        if (location.getZone() != null && !location.getZone().isBlank()) {
            document.add(new Paragraph("Strefa: " + location.getZone())
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        document.add(new Paragraph("Typ: " + location.getType().name())
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER));

        if (location.getBarcode() != null) {
            byte[] barcodePng = barcodeService.generateBarcodeImage(location.getBarcode());
            Image barcodeImg = new Image(ImageDataFactory.create(barcodePng));
            barcodeImg.setMaxWidth(200);
            barcodeImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(barcodeImg);
        }

        if (location.getQrData() != null) {
            byte[] qrPng = barcodeService.generateQrImage(location.getQrData());
            Image qrImg = new Image(ImageDataFactory.create(qrPng));
            qrImg.setMaxWidth(80);
            qrImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(qrImg);
        }
    }

    private void addLocationCellContent(Cell cell, Location location) {
        cell.add(new Paragraph("LOKALIZACJA")
                .setBold()
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER));

        cell.add(new Paragraph(location.getName())
                .setBold()
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));

        cell.add(new Paragraph("Kod: " + location.getCode())
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));

        if (location.getZone() != null && !location.getZone().isBlank()) {
            cell.add(new Paragraph("Strefa: " + location.getZone())
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        cell.add(new Paragraph("Typ: " + location.getType().name())
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));

        if (location.getBarcode() != null) {
            byte[] barcodePng = barcodeService.generateBarcodeImage(location.getBarcode());
            Image barcodeImg = new Image(ImageDataFactory.create(barcodePng));
            barcodeImg.setMaxWidth(160);
            barcodeImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            cell.add(barcodeImg);
        }

        if (location.getQrData() != null) {
            byte[] qrPng = barcodeService.generateQrImage(location.getQrData());
            Image qrImg = new Image(ImageDataFactory.create(qrPng));
            qrImg.setMaxWidth(60);
            qrImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            cell.add(qrImg);
        }
    }

    private void addProductLabelContent(Document document, Product product) {
        document.add(new Paragraph("PRODUKT")
                .setBold()
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(product.getName())
                .setBold()
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("SKU: " + product.getSku())
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER));

        String barcodeText = product.getBarcode() != null
                ? product.getBarcode()
                : "PROD-" + product.getId();

        byte[] barcodePng;
        if (product.getBarcode() != null) {
            barcodePng = barcodeService.generateBarcodeImage(product.getBarcode());
        } else {
            barcodePng = barcodeService.generateBarcodeImage(barcodeText);
        }

        Image barcodeImg = new Image(ImageDataFactory.create(barcodePng));
        barcodeImg.setMaxWidth(200);
        barcodeImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        document.add(barcodeImg);
    }
}
