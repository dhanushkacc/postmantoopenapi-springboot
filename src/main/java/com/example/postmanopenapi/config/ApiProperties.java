package com.example.postmanopenapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "postman.api")
@Getter
@Setter
public class ApiProperties {
    private String key;
    private String baseUrl;
}
