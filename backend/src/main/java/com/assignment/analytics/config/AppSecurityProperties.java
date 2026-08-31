package com.assignment.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(String jwtSecret, long jwtTtlMinutes) {
}
