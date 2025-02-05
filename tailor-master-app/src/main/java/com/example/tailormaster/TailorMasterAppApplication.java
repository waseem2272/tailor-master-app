package com.example.tailormaster;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class TailorMasterAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(TailorMasterAppApplication.class, args);
	}
}
