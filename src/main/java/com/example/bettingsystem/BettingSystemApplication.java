package com.example.bettingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"com.example.*"})
public class BettingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BettingSystemApplication.class, args);
    }

}
