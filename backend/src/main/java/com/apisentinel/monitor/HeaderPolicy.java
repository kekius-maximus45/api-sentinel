package com.apisentinel.monitor;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HeaderPolicy {
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "proxy-authorization",
            "x-api-key",
            "x-auth-token"
    );

    private HeaderPolicy() {
    }

    public static Map<String, String> redact(Map<String, String> headers) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                sanitized.put(name, "[redacted]");
            } else {
                sanitized.put(name, value);
            }
        });
        return sanitized;
    }
}
