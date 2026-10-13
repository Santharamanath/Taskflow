package com.example.demo;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

    @PostConstruct
    public void init() {
        // Enforce IST Timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        // Force Java to use IPv4 network stack instead of unreachable IPv6
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication.run(DemoApplication.class, args);
    }
}