package com.heisenbug.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ClaimsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimsBackendApplication.class, args);
    }
}
