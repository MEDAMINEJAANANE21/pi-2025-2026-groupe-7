package fr.uga.miage.l3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PIApplication {

    public static void main(String[] args) {
        SpringApplication.run(PIApplication.class, args);
    }
}