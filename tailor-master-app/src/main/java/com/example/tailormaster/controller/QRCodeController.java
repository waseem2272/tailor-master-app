package com.example.tailormaster.controller;

import com.example.tailormaster.service.QRCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class QRCodeController {

    private QRCodeService qrCodeService;

    public QRCodeController(QRCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/generate-qrcode")
    public String generateQRCode(@RequestParam String data) {
        String filePath = "E:\\qrcode.png"; // Path where the QR code image will be saved
        try {
            qrCodeService.generateQRCode(data, filePath);
            return "QR Code generated successfully! Check the file: " + filePath;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating QR Code: " + e.getMessage();
        }
    }
}
