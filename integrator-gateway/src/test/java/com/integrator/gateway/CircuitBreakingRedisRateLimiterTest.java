package com.integrator.gateway;

import com.integrator.gateway.ratelimit.CircuitBreakingRedisRateLimiter;
import org.junit.jupiter.api.*;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CircuitBreakingRedisRateLimiterTest {
    private static final String ROUTE_ID = "route-1";
    private static final String RATE_LIMIT_KEY = "route:route-1";
    private static final String X_RATELIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String REDIS_STATUS_HEADER = "X-RateLimit-Redis";

    private RedisRateLimiter redisRateLimiter;
    private CircuitBreakingRedisRateLimiter circuitBreakingRedisRateLimiter;

    @BeforeEach
    void setUp() {
        redisRateLimiter = mock(RedisRateLimiter.class);
        circuitBreakingRedisRateLimiter = new CircuitBreakingRedisRateLimiter(redisRateLimiter);
    }

    @Test
    @Order(1)
    @DisplayName("Delegates to RedisRateLimiter when Redis returns a normal response")
    void delegatesWhenRedisReturnsNormalResponse() {
        RateLimiter.Response redisResponse = new RateLimiter.Response(true, Map.of(X_RATELIMIT_REMAINING, "9"));
        when(redisRateLimiter.isAllowed(ROUTE_ID, RATE_LIMIT_KEY)).thenReturn(Mono.just(redisResponse));

        StepVerifier.create(circuitBreakingRedisRateLimiter.isAllowed(ROUTE_ID, RATE_LIMIT_KEY))
                .assertNext(response -> assertThat(response).isSameAs(redisResponse))
                .verifyComplete();

        verify(redisRateLimiter).isAllowed(ROUTE_ID, RATE_LIMIT_KEY);
    }

    @Test
    @Order(2)
    @DisplayName("Caches Redis outage when RedisRateLimiter returns remaining minus one")
    void cachesRedisOutageWhenRedisRateLimiterReturnsRemainingMinusOne() {
        when(redisRateLimiter.isAllowed(ROUTE_ID, RATE_LIMIT_KEY))
                .thenReturn(Mono.just(new RateLimiter.Response(true, Map.of(X_RATELIMIT_REMAINING, "-1"))));

        StepVerifier.create(circuitBreakingRedisRateLimiter.isAllowed(ROUTE_ID, RATE_LIMIT_KEY))
                .assertNext(response -> assertThat(response.getHeaders())
                        .containsEntry(REDIS_STATUS_HEADER, "DOWN"))
                .verifyComplete();

        StepVerifier.create(circuitBreakingRedisRateLimiter.isAllowed(ROUTE_ID, RATE_LIMIT_KEY))
                .assertNext(response -> assertThat(response.getHeaders())
                        .containsEntry(REDIS_STATUS_HEADER, "DOWN_CACHED"))
                .verifyComplete();

        verify(redisRateLimiter, times(1)).isAllowed(ROUTE_ID, RATE_LIMIT_KEY);
    }

    @Test
    @Order(3)
    @DisplayName("Propagates RedisRateLimiter errors")
    void propagatesRedisRateLimiterErrors() {
        when(redisRateLimiter.isAllowed(ROUTE_ID, RATE_LIMIT_KEY))
                .thenReturn(Mono.error(new TimeoutException("redis timed out")));

        StepVerifier.create(circuitBreakingRedisRateLimiter.isAllowed(ROUTE_ID, RATE_LIMIT_KEY))
                .expectError(TimeoutException.class)
                .verify();

        verify(redisRateLimiter).isAllowed(ROUTE_ID, RATE_LIMIT_KEY);
    }

    @Test
    @Order(4)
    @DisplayName("Delegates config methods to RedisRateLimiter")
    void delegatesConfigMethodsToRedisRateLimiter() {
        RedisRateLimiter.Config config = new RedisRateLimiter.Config();
        config.setReplenishRate(10);
        config.setBurstCapacity(20);
        config.setRequestedTokens(1);
        Map<String, RedisRateLimiter.Config> configMap = Map.of(ROUTE_ID, config);
        when(redisRateLimiter.getConfigClass()).thenReturn(RedisRateLimiter.Config.class);
        when(redisRateLimiter.newConfig()).thenReturn(config);
        when(redisRateLimiter.getConfig()).thenReturn(configMap);

        assertThat(circuitBreakingRedisRateLimiter.getConfigClass()).isEqualTo(RedisRateLimiter.Config.class);
        assertThat(circuitBreakingRedisRateLimiter.newConfig()).isSameAs(config);
        assertThat(circuitBreakingRedisRateLimiter.getConfig()).isSameAs(configMap);
    }
}
