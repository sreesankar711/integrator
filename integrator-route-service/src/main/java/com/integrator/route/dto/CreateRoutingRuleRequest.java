package com.integrator.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRoutingRuleRequest {
    @NotBlank
    private String matchConfig;

    @NotBlank
    @Size(max = 500)
    private String overrideTargetUrl;

    @NotNull
    private Integer priority;

    @NotNull
    private Boolean enabled;
}
