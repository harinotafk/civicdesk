package com.ram.civicdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class CivicdeskApplication {

    public static void main(String[] args) {
        // All deadlines are Chennai time, whatever timezone the server runs in.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(CivicdeskApplication.class, args);
    }
}
