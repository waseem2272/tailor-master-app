package com.example.tailormaster.util;

import org.springframework.stereotype.Component;

@Component
public class ThymeleafUtil {
    public String encryptId(Long id) {
        return AESUtil.encrypt(String.valueOf(id));
    }
}
