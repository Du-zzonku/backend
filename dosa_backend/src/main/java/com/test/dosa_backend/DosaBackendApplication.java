package com.test.dosa_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DosaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DosaBackendApplication.class, args);
    }

}
