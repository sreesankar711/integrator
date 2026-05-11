package com.integrator.route.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRuleResponse {
    private UUID id ;
    private UUID routeId ;
    private String matchConfig ;
    private String overrideTargetUrl ;
    private Integer priority ;
    private boolean enabled ;
    private Instant createdAt;
}
