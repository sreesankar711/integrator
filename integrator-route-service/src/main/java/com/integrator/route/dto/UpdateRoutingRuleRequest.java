package com.integrator.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoutingRuleRequest {
    @NotBlank
    private String matchConfig;

    @NotBlank
    @Size(max = 500)
    private String overrideTargetUrl;

    private Integer priority = 0;
    private Boolean enabled = true;
}
