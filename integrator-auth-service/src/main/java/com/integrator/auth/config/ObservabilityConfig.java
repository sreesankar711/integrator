package com.integrator.auth.config;

import com.integrator.common.observability.ApiLoggingFilter;
import com.integrator.common.observability.CorrelationIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ObservabilityConfig {
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> filterFilterRegistrationBean = new FilterRegistrationBean<>(new CorrelationIdFilter());
        filterFilterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterFilterRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean<ApiLoggingFilter> apiLoggingFilter(ObjectMapper objectMapper, @Value("${spring.application.name}") String applicationName) {
        FilterRegistrationBean<ApiLoggingFilter> filterFilterRegistrationBean = new FilterRegistrationBean<>(new ApiLoggingFilter(objectMapper, applicationName));
        filterFilterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return filterFilterRegistrationBean;
    }
}
