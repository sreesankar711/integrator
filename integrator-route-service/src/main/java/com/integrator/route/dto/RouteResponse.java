package com.integrator.route.dto;

import com.integrator.route.model.TransformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private UUID id ;
    private String name ;
    private String description ;
    private String pathPattern ;
    private RequestMethod httpMethod ;
    private String targetUrl ;
    private TransformType transformType ;
    private String fieldMappingConfig ;
    private UUID snippetId ;
    private boolean enabled ;
    private Instant createdAt ;
    private Instant updatedAt ;
    private List<RoutingRuleResponse> routingRules;
}
