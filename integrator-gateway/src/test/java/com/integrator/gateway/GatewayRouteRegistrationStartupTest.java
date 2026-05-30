package com.integrator.gateway;

import com.integrator.gateway.event.RouteEventConsumerReadiness;
import com.integrator.gateway.service.GatewayRouteRegistrationService;
import com.integrator.gateway.startup.GatewayRouteRegistrationStartup;
import org.junit.jupiter.api.*;
import org.mockito.InOrder;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayRouteRegistrationStartupTest {
    private final GatewayRouteRegistrationService gatewayRouteRegistrationService = mock(GatewayRouteRegistrationService.class);
    private final RouteEventConsumerReadiness routeEventConsumerReadiness = mock(RouteEventConsumerReadiness.class);
    private final GatewayRouteRegistrationStartup gatewayRouteRegistrationStartup = new GatewayRouteRegistrationStartup(
            gatewayRouteRegistrationService,
            routeEventConsumerReadiness
    );

    @Test
    @Order(1)
    @DisplayName("Routes are registered after route event consumer is ready")
    void registerRoutesSuccessAfterConsumerIsReady() {
        ApplicationArguments applicationArguments = mock(ApplicationArguments.class);

        gatewayRouteRegistrationStartup.run(applicationArguments);

        InOrder inOrder = inOrder(routeEventConsumerReadiness, gatewayRouteRegistrationService);
        inOrder.verify(routeEventConsumerReadiness).awaitReady();
        inOrder.verify(gatewayRouteRegistrationService).registerRoutes();
    }
}
