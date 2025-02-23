package com.example.tailormaster.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {

    private static final String SECRET_KEY = "1234567890123456"; // Store securely!
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    public static String encrypt(String value) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);

            // Generate random IV for each encryption
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(value.getBytes());

            // Encode IV + Encrypted Data to Base64
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getUrlEncoder().encodeToString(combined); // URL safe
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting", e);
        }
    }

    public static String decrypt(String encryptedValue) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);

            byte[] decodedBytes = Base64.getUrlDecoder().decode(encryptedValue);

            // Extract IV and Encrypted Data
            byte[] iv = new byte[16];
            byte[] encryptedData = new byte[decodedBytes.length - 16];
            System.arraycopy(decodedBytes, 0, iv, 0, iv.length);
            System.arraycopy(decodedBytes, iv.length, encryptedData, 0, encryptedData.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(encryptedData);

            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting", e);
        }
    }
}

