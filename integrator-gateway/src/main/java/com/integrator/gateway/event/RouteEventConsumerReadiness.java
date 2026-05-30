package com.integrator.gateway.event;

import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RouteEventConsumerReadiness {
    private final CompletableFuture<Void> ready = new CompletableFuture<>();

    public void markReady() {
        ready.complete(null);
    }
    public void awaitReady() {
        try {
            ready.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for route event consumer", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Route event consumer was not ready in time", e);
        }
    }
}