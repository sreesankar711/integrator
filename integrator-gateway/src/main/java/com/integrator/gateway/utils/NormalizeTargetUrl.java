package com.integrator.gateway.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NormalizeTargetUrl {
    private static final Pattern HAS_SCHEME = Pattern.compile("(?i)^[a-z][a-z0-9+.-]*://.*");

    public static Optional<URI> getURL(String targetUrl) {
        if (!StringUtils.hasText(targetUrl)) {
            return Optional.empty();
        }
        String normalizedTargetUrl = targetUrl.trim();
        if (!HAS_SCHEME.matcher(normalizedTargetUrl).matches()) {
            normalizedTargetUrl = "http://" + normalizedTargetUrl;
        }
        try {
            URI uri = URI.create(normalizedTargetUrl);
            if (!uri.isAbsolute()
                    || uri.getHost() == null
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return Optional.empty();
            }
            return Optional.of(uri);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
