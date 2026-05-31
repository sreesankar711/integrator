package com.integrator.gateway.startup;

import com.integrator.gateway.event.RouteEventConsumerReadiness;
import com.integrator.gateway.service.GatewayRouteRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
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

    @Scheduled(fixedDelayString = "${gateway.route-refresh-interval}", initialDelayString = "${gateway.route-refresh-interval}")
    public void refreshRoutes() {
        gatewayRouteRegistrationService.refreshRoutes();
    }
}
