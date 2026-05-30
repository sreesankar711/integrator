package com.integrator.gateway.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class RoutingRule {
    private UUID id ;
    private UUID routeId ;
    private String matchConfig ;
    private String overrideTargetUrl ;
    private Integer priority ;
    private boolean enabled ;
    private Instant createdAt;
}
