package com.integrator.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayClientConfig {
    @Bean
    public WebClient routeServiceWebClient(GatewayProperties gatewayProperties) {
        return WebClient.builder()
                .baseUrl(gatewayProperties.getRouteServiceUrl())
                .build();
    }
}
