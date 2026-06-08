package com.integrator.route.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "path_pattern", nullable = false, length = 500)
    private String pathPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    private RequestMethod httpMethod;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "transform_type", nullable = false, length = 30)
    private TransformType transformType = TransformType.NONE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_mapping_config", columnDefinition = "jsonb")
    private String fieldMappingConfig;

    @Column(name = "snippet_id")
    private UUID snippetId;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "rate_limit_enabled", nullable = false)
    private boolean rateLimitEnabled;

    @Column(name = "rate_limit_replenish_rate")
    private Integer rateLimitReplenishRate;

    @Column(name = "rate_limit_burst_capacity")
    private Integer rateLimitBurstCapacity;

    @Column(name = "rate_limit_requested_tokens")
    private Integer rateLimitRequestedTokens;
}

