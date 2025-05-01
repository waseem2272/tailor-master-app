package com.example.tailormaster;

import com.example.tailormaster.util.AESUtil;
import com.example.tailormaster.util.ThymeleafUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TailorMasterAppApplication implements CommandLineRunner {
	public static void main(String[] args) {
		SpringApplication.run(TailorMasterAppApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
//		System.out.println(AESUtil.decrypt("omXUkMjgEbjR5NCUNwF3rwujuwbQYZHtOMuEgbk1sTI="));
	}
}
