package com.splitwise.sim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
public class SplitwiseSimApplication {
    public static void main(String[] args) {
        SpringApplication.run(SplitwiseSimApplication.class, args);
        System.out.println("Splitwise Backend Started on http://localhost:8081");
    }
}