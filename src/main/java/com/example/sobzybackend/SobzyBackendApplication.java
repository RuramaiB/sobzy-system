package com.example.sobzybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SobzyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SobzyBackendApplication.class, args);
    }

}
