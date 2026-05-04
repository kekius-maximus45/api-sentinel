package com.apisentinel.check;

import com.apisentinel.monitor.Monitor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class CheckExecutor {
    private final WebClient.Builder webClientBuilder;

    public CheckExecutor(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public CheckResult execute(Monitor monitor, Map<String, String> headers) {
        Instant checkedAt = Instant.now();
        long started = System.nanoTime();
        try {
            WebClient.RequestBodySpec request = webClientBuilder.build()
                    .method(HttpMethod.valueOf(monitor.getMethod().name()))
                    .uri(monitor.getUrl());
            headers.forEach((name, value) -> request.header(name, value));
            Mono<RawHttpResult> responseMono;
            if (monitor.getBody() != null && !monitor.getBody().isBlank() && supportsBody(monitor)) {
                responseMono = request.bodyValue(monitor.getBody()).exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new RawHttpResult(response.statusCode().value(), body)));
            } else {
                responseMono = request.exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new RawHttpResult(response.statusCode().value(), body)));
            }
            RawHttpResult raw = responseMono
                    .timeout(Duration.ofSeconds(monitor.getTimeoutSeconds()))
                    .block(Duration.ofSeconds(monitor.getTimeoutSeconds() + 1L));
            int latencyMs = Math.toIntExact(Duration.ofNanos(System.nanoTime() - started).toMillis());
            boolean success = raw != null && raw.statusCode() == monitor.getExpectedStatusCode();
            return new CheckResult(
                    checkedAt,
                    latencyMs,
                    raw == null ? null : raw.statusCode(),
                    success,
                    success ? null : "UNEXPECTED_STATUS",
                    snippet(raw == null ? "" : raw.body())
            );
        } catch (Exception exception) {
            int latencyMs = Math.toIntExact(Duration.ofNanos(System.nanoTime() - started).toMillis());
            return new CheckResult(
                    checkedAt,
                    latencyMs,
                    null,
                    false,
                    categorize(exception),
                    snippet(exception.getMessage())
            );
        }
    }

    private boolean supportsBody(Monitor monitor) {
        return switch (monitor.getMethod()) {
            case POST, PUT, PATCH -> true;
            default -> false;
        };
    }

    private String categorize(Exception exception) {
        String name = exception.getClass().getSimpleName().toLowerCase();
        if (name.contains("timeout")) {
            return "TIMEOUT";
        }
        if (name.contains("illegalargument")) {
            return "CONFIGURATION";
        }
        return "NETWORK";
    }

    private String snippet(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    private record RawHttpResult(int statusCode, String body) {
    }
}
