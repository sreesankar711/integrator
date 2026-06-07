package com.integrator.route.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private Resource publicKeyPath;
}
