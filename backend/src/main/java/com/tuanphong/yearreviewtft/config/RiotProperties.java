package com.tuanphong.yearreviewtft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "riot.api")
public record RiotProperties(String key) {}
