package com.integrator.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bootstrap.admin")
public class BootstrapAdminProperties {
    private boolean enabled = false;
    private String username;
    private String email;
    private String password;
}
