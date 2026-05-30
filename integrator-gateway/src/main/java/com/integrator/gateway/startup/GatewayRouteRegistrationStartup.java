package com.integrator.gateway.startup;

import com.integrator.gateway.event.RouteEventConsumerReadiness;
import com.integrator.gateway.service.GatewayRouteRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatewayRouteRegistrationStartup implements ApplicationRunner {
    private final GatewayRouteRegistrationService gatewayRouteRegistrationService;
    private final RouteEventConsumerReadiness routeEventConsumerReadiness;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        routeEventConsumerReadiness.awaitReady();
        gatewayRouteRegistrationService.registerRoutes();
    }
}
