package com.integrator.route;

import com.integrator.route.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = "com.integrator")
@EnableConfigurationProperties(JwtProperties.class)
public class IntegratorRouteServiceApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(IntegratorRouteServiceApplication.class, args);
	}

}
