package com.apisentinel.monitor;

import com.apisentinel.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitors")
public class Monitor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitorHttpMethod method;

    @Column(nullable = false)
    private int expectedStatusCode;

    @Column(nullable = false)
    private int timeoutSeconds;

    @Column(nullable = false)
    private int intervalSeconds;

    @Column(nullable = false)
    private int latencyThresholdMs;

    @Column(nullable = false)
    private int failureThreshold;

    @Column(nullable = false)
    private int consecutiveFailures;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitorState state;

    @Column(columnDefinition = "text")
    private String headersJson;

    @Column(columnDefinition = "text")
    private String body;

    private Instant lastCheckedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Monitor() {
    }

    public Monitor(Project project, String name, String url, MonitorHttpMethod method, int expectedStatusCode,
                   int timeoutSeconds, int intervalSeconds, int latencyThresholdMs, int failureThreshold,
                   String headersJson, String body) {
        this.project = project;
        this.name = name;
        this.url = url;
        this.method = method;
        this.expectedStatusCode = expectedStatusCode;
        this.timeoutSeconds = timeoutSeconds;
        this.intervalSeconds = intervalSeconds;
        this.latencyThresholdMs = latencyThresholdMs;
        this.failureThreshold = failureThreshold;
        this.headersJson = headersJson;
        this.body = body;
        this.enabled = true;
        this.state = MonitorState.UP;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateConfiguration(String name, String url, MonitorHttpMethod method, int expectedStatusCode,
                                    int timeoutSeconds, int intervalSeconds, int latencyThresholdMs,
                                    int failureThreshold, String headersJson, String body, boolean enabled) {
        this.name = name;
        this.url = url;
        this.method = method;
        this.expectedStatusCode = expectedStatusCode;
        this.timeoutSeconds = timeoutSeconds;
        this.intervalSeconds = intervalSeconds;
        this.latencyThresholdMs = latencyThresholdMs;
        this.failureThreshold = failureThreshold;
        this.headersJson = headersJson;
        this.body = body;
        this.enabled = enabled;
        if (!enabled) {
            this.state = MonitorState.PAUSED;
        }
    }

    public void markPaused() {
        enabled = false;
        state = MonitorState.PAUSED;
    }

    public void markEnabled() {
        enabled = true;
        if (state == MonitorState.PAUSED) {
            state = MonitorState.UP;
        }
    }

    public void recordState(MonitorState state, int consecutiveFailures, Instant checkedAt) {
        this.state = state;
        this.consecutiveFailures = consecutiveFailures;
        this.lastCheckedAt = checkedAt;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public MonitorHttpMethod getMethod() {
        return method;
    }

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public int getLatencyThresholdMs() {
        return latencyThresholdMs;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MonitorState getState() {
        return state;
    }

    public String getHeadersJson() {
        return headersJson;
    }

    public String getBody() {
        return body;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
