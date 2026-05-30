package com.integrator.route.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.integrator.common.event.RouteEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteKafkaEventPublisher {
    private final KafkaTemplate<String, RouteEvent> kafkaTemplate;

    @Value("${route.events.topic}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(RouteEvent routeEvent) {
        kafkaTemplate.send(topic, routeEvent.getRouteId().toString(), routeEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish route event: routeId={}, eventType={}",
                                routeEvent.getRouteId(), routeEvent.getRouteEventType(), ex);
                    }
                });
    }
}