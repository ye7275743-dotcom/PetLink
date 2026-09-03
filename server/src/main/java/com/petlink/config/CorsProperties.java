package com.petlink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "petlink.cors")
public class CorsProperties {
    @NotEmpty
    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
}
