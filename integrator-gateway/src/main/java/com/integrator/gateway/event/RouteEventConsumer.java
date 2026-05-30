package com.integrator.gateway.event;

import com.integrator.common.event.RouteEvent;
import com.integrator.gateway.config.GatewayProperties;
import com.integrator.gateway.service.GatewayRouteRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteEventConsumer implements ConsumerSeekAware {
    private final GatewayRouteRegistrationService routeRegistrationService;
    private final GatewayProperties gatewayProperties;
    private final RouteEventConsumerReadiness routeEventConsumerReadiness;

    public String getRouteEventsTopic() {
        return gatewayProperties.getRouteEventsTopic();
    }

    @KafkaListener(topics = "#{__listener.routeEventsTopic}")
    public void onRouteEvent(RouteEvent event) {
        log.info("Received route event: eventId={}, type={}, routeId={}",
                event.getEventId(), event.getRouteEventType(), event.getRouteId());
        switch (event.getRouteEventType()) {
            case CREATED, UPDATED -> routeRegistrationService.createOrUpdateRoute(event.getRouteId());
            case DELETED -> routeRegistrationService.deleteRoute(event.getRouteId());
        }
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToEnd(assignments.keySet());
        routeEventConsumerReadiness.markReady();
        log.info("seeking to end in kafka");
    }
}
