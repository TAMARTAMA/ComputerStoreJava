package com.tamar.computerstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ComputerStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComputerStoreApplication.class, args);
    }
}
