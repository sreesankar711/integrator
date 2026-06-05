package com.integrator.gateway;

import com.integrator.gateway.utils.NormalizeTargetUrl;
import org.junit.jupiter.api.*;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NormalizeTargetUrlTest {

    @Test
    @Order(1)
    @DisplayName("Missing scheme is normalized to http")
    void getUrlSuccessMissingScheme() {
        assertThat(NormalizeTargetUrl.getURL("localhost:9000/order/3")).hasValue(URI.create("http://localhost:9000/order/3"));
    }

    @Test
    @Order(2)
    @DisplayName("Https URL is kept")
    void getUrlSuccessHttps() {
        assertThat(NormalizeTargetUrl.getURL("https://example.com/orders")).hasValue(URI.create("https://example.com/orders"));
    }

    @Test
    @Order(3)
    @DisplayName("Blank URL returns empty")
    void getUrlFailureBlank() {
        assertThat(NormalizeTargetUrl.getURL(" ")).isEmpty();
    }

    @Test
    @Order(4)
    @DisplayName("Unsupported scheme returns empty")
    void getUrlFailureUnsupportedScheme() {
        assertThat(NormalizeTargetUrl.getURL("ftp://example.com/orders")).isEmpty();
    }

    @Test
    @Order(5)
    @DisplayName("URL without host returns empty")
    void getUrlFailureMissingHost() {
        assertThat(NormalizeTargetUrl.getURL("https://")).isEmpty();
    }
}
