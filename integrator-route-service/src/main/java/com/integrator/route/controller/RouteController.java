package com.integrator.route.controller;

import com.integrator.common.api.ApiResponse;
import com.integrator.common.api.PagedResponse;
import com.integrator.route.dto.CreateRouteRequest;
import com.integrator.route.dto.RouteResponse;
import com.integrator.route.dto.UpdateRouteRequest;
import com.integrator.route.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static com.integrator.route.util.Utils.parseRouteId;


@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteController {
    private final RouteService routeService;

    @PostMapping
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(@RequestBody @Valid CreateRouteRequest createRouteRequest) {
        RouteResponse response = routeService.createRoute(createRouteRequest);
        return ResponseEntity.created(URI.create("/routes/" + response.getId()))
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getAllRoutes() {
        return ResponseEntity.ok(ApiResponse.success(routeService.getAllRoutes()));
    }

    @GetMapping(params = {"page", "size"})
    public ResponseEntity<PagedResponse<RouteResponse>> getAllRoutes(@RequestParam("page") int page, @RequestParam("size") int size) {
        Page<RouteResponse> routePage = routeService.getAllRoutes(page, size);
        return ResponseEntity.ok(PagedResponse.success(
                routePage.getContent(),
                routePage.getNumber(),
                routePage.getSize(),
                routePage.getTotalElements(),
                routePage.getTotalPages(),
                routePage.isLast())
        );
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(@PathVariable("routeId") String routeId) {
            return ResponseEntity.ok(ApiResponse.success(routeService.getRoute(parseRouteId(routeId))));
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(@PathVariable("routeId") String routeId, @RequestBody @Valid UpdateRouteRequest updateRouteRequest) {
            return ResponseEntity.ok(ApiResponse.success(routeService.updateRoute(parseRouteId(routeId), updateRouteRequest)));
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable("routeId") String routeId) {
            routeService.deleteRoute(parseRouteId(routeId));
            return ResponseEntity.ok(ApiResponse.success(null));
    }
}




