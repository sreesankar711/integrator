package com.integrator.gateway.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Primary
@RequiredArgsConstructor
public class CircuitBreakingRedisRateLimiter implements RateLimiter<RedisRateLimiter.Config> {
    private final RedisRateLimiter redisRateLimiter;
    private final AtomicLong redisDownUntil = new AtomicLong(0);
    private final Duration redisCallTimeout = Duration.ofMillis(500);
    private final Duration outageCacheTtl = Duration.ofSeconds(10);
    private static final String X_RATELIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String REDIS_STATUS_HEADER = "X-RateLimit-Redis";

    @Override
    public Mono<RateLimiter.Response> isAllowed(String routeId, String id) {
        long now = System.currentTimeMillis();
        if (now < redisDownUntil.get()) {
            return Mono.just(new RateLimiter.Response(true, Map.of(REDIS_STATUS_HEADER, "DOWN_CACHED")));
        }
        return redisRateLimiter.isAllowed(routeId, id)
                .timeout(redisCallTimeout)
                .flatMap(response -> {
                    if ("-1".equals(response.getHeaders().get(X_RATELIMIT_REMAINING))) {
                        redisDownUntil.set(System.currentTimeMillis() + outageCacheTtl.toMillis());
                        return Mono.just(new RateLimiter.Response(true, Map.of(REDIS_STATUS_HEADER, "DOWN")));
                    }
                    redisDownUntil.set(0);
                    return Mono.just(response);
                });
    }

    @Override
    public Class<RedisRateLimiter.Config> getConfigClass() {
        return redisRateLimiter.getConfigClass();
    }

    @Override
    public RedisRateLimiter.Config newConfig() {
        return redisRateLimiter.newConfig();
    }

    @Override
    public Map<String, RedisRateLimiter.Config> getConfig() {
        return redisRateLimiter.getConfig();
    }
}
