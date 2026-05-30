package com.integrator.gateway;

import com.integrator.common.event.RouteEvent;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(properties = {
        "gateway.route-events-topic=route.events",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class AbstractContainerBaseTest {
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:latest");

    static {
        kafka.start();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    KafkaTemplate<String, RouteEvent> kafkaTemplate;

}
