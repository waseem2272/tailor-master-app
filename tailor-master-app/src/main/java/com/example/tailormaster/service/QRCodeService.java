package com.example.tailormaster.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class QRCodeService {

    public void generateQRCode(String data, String filePath) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 300, 300);

        Path path = new File(filePath).toPath();
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }
}
