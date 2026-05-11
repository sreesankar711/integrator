package com.integrator.route.util;

import com.integrator.common.exception.ResourceNotFoundException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {
    private static final String ROUTE_NOT_FOUND = "Route not found";
    private static final String RULE_NOT_FOUND = "Routing rule not found";

    private static UUID parseUUID(String uuid, String message) {
        if (uuid == null)
            throw new ResourceNotFoundException(message);
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(message);
        }
    }

    public static UUID parseRouteId(String routeId) {
        return parseUUID(routeId, ROUTE_NOT_FOUND);
    }

    public  static UUID parseRuleId(String ruleId) {
        return parseUUID(ruleId, RULE_NOT_FOUND);
    }
}
