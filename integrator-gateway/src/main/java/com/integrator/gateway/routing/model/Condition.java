package com.integrator.gateway.routing.model;

import lombok.Data;

@Data
public class Condition {
    private RouteConditionType type;
    private String key;
    private String equals;
}