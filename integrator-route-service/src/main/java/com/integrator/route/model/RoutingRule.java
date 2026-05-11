package com.integrator.route.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "routing_rules")
public class RoutingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "match_config", nullable = false, columnDefinition = "jsonb")
    private String matchConfig;

    @Column(name = "override_target_url", length = 500,  nullable = false)
    private String overrideTargetUrl;

    @Builder.Default
    @Column(nullable = false)
    private Integer priority = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}