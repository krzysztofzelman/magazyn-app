package com.example.magazyn.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.oned.Code128Writer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class BarcodeService {

    private static final int BARCODE_WIDTH = 300;
    private static final int BARCODE_HEIGHT = 100;
    private static final int QR_SIZE = 300;

    public byte[] generateBarcodeImage(String barcodeText) {
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(barcodeText, BarcodeFormat.CODE_128,
                    BARCODE_WIDTH, BARCODE_HEIGHT);
            return writeToPng(bitMatrix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate barcode image", e);
        }
    }

    public byte[] generateQrImage(String qrData) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE,
                    QR_SIZE, QR_SIZE);
            return writeToPng(bitMatrix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }

    private byte[] writeToPng(BitMatrix bitMatrix) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write barcode image to stream", e);
        }
    }
}
