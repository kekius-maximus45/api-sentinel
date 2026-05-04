package com.apisentinel.check;

import java.time.Instant;

public record CheckResult(
        Instant checkedAt,
        Integer latencyMs,
        Integer statusCode,
        boolean success,
        String errorCategory,
        String responseSnippet
) {
}
