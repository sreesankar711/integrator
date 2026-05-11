package com.integrator.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private Resource  privateKeyPath;
    private Resource publicKeyPath;
    private Duration accessTokenExpiry = Duration.ofMinutes(15);
    private Duration refreshTokenExpiry = Duration.ofDays(7);

}
