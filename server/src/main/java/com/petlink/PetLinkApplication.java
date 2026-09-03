package com.petlink;

import com.petlink.config.CorsProperties;
import com.petlink.infrastructure.file.service.FileProperties;
import com.petlink.security.JwtProperties;
import com.petlink.modules.clue.config.ClueApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, FileProperties.class, CorsProperties.class, ClueApiProperties.class})
public class PetLinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetLinkApplication.class, args);
    }
}
