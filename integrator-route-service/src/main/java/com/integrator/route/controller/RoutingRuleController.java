package com.integrator.route.controller;

import com.integrator.common.api.ApiResponse;
import com.integrator.route.dto.CreateRoutingRuleRequest;
import com.integrator.route.dto.RoutingRuleResponse;
import com.integrator.route.dto.UpdateRoutingRuleRequest;
import com.integrator.route.service.RoutingRuleService;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static com.integrator.route.util.Utils.parseRouteId;
import static com.integrator.route.util.Utils.parseRuleId;

@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RoutingRuleController {
    private final RoutingRuleService routingRuleService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<RoutingRuleResponse>> createRule(@Nullable @RequestParam("routeId") String routeId, @RequestBody @Valid CreateRoutingRuleRequest request) {
            RoutingRuleResponse response = routingRuleService.createRoutingRule(parseRouteId(routeId), request);
            return ResponseEntity.created(URI.create("/rules/" + response.getId()))
                    .body(ApiResponse.success(response));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<RoutingRuleResponse>> updateRule(@PathVariable("ruleId") String ruleId, @RequestBody @Valid UpdateRoutingRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(routingRuleService.updateRoutingRule(parseRuleId(ruleId), request)));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable("ruleId") String ruleId) {
        routingRuleService.deleteRoutingRule(parseRuleId(ruleId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
