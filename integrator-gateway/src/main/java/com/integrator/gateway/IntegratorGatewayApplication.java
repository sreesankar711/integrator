package com.integrator.gateway;

import com.integrator.gateway.config.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class IntegratorGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(IntegratorGatewayApplication.class, args);
	}

}
