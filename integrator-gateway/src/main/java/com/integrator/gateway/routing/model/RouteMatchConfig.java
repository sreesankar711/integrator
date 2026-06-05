package com.integrator.gateway.routing.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RouteMatchConfig {
    private RouteMatchMode matchMode = RouteMatchMode.ALL;
    private List<Condition> conditions = new ArrayList<>();
}