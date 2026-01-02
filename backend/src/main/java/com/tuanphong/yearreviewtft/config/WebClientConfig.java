package com.tuanphong.yearreviewtft.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient riotWebClient(RiotProperties riotProperties) {
        return WebClient.builder()
                .defaultHeader("X-Riot-Token", riotProperties.key())
                .build();
    }
}

