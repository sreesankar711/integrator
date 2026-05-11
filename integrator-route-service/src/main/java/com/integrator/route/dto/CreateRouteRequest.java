package com.integrator.route.dto;

import com.integrator.route.model.TransformType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.UUID;

@Data
public class CreateRouteRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 500)
    private String pathPattern;

    @NotNull
    private RequestMethod httpMethod;

    @NotBlank
    @Size(max = 500)
    private String targetUrl;

    @NotNull
    private TransformType transformType;

    private String fieldMappingConfig;

    private UUID snippetId;

    @NotNull
    private Boolean enabled;
}
