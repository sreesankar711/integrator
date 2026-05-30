package com.integrator.gateway.dto;

import com.integrator.gateway.model.RoutingRule;
import com.integrator.gateway.model.TransformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    private UUID id;
    private String name;
    private String pathPattern;
    private RequestMethod httpMethod;
    private String targetUrl;
    private TransformType transformType;
    private String fieldMappingConfig;
    private UUID snippetId;
    private boolean enabled;
    private List<RoutingRule> routingRules;
}