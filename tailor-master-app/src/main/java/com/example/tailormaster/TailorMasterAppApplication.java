package com.example.tailormaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TailorMasterAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(TailorMasterAppApplication.class, args);
	}
}
