package com.integrator.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private String routeServiceUrl;
    private String routeEventsTopic;
}
