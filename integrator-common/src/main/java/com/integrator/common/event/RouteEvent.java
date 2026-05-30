package com.integrator.common.event;

import com.integrator.common.observability.CorrelationConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteEvent {
    @Builder.Default
    private String eventId  = MDC.get(CorrelationConstants.MDC_CORRELATION_ID_KEY);
    private RouteEventType routeEventType;
    private UUID routeId;

    public static RouteEvent of(RouteEventType routeEventType, UUID routeId) {
        return RouteEvent.builder()
                .routeEventType(routeEventType)
                .routeId(routeId)
                .build();
    }
}
