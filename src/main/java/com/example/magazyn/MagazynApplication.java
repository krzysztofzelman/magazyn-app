package com.example.magazyn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MagazynApplication {

	public static void main(String[] args) {
		SpringApplication.run(MagazynApplication.class, args);
	}

}
