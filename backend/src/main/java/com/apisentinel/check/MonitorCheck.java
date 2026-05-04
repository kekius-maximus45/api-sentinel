package com.apisentinel.check;

import com.apisentinel.monitor.Monitor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitor_checks")
public class MonitorCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id")
    private Monitor monitor;

    @Column(nullable = false)
    private Instant checkedAt;

    private Integer latencyMs;

    private Integer statusCode;

    @Column(nullable = false)
    private boolean success;

    private String errorCategory;

    @Column(columnDefinition = "text")
    private String responseSnippet;

    protected MonitorCheck() {
    }

    public MonitorCheck(Monitor monitor, Instant checkedAt, Integer latencyMs, Integer statusCode,
                        boolean success, String errorCategory, String responseSnippet) {
        this.monitor = monitor;
        this.checkedAt = checkedAt;
        this.latencyMs = latencyMs;
        this.statusCode = statusCode;
        this.success = success;
        this.errorCategory = errorCategory;
        this.responseSnippet = responseSnippet;
    }

    @PrePersist
    void onCreate() {
        if (checkedAt == null) {
            checkedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public String getResponseSnippet() {
        return responseSnippet;
    }
}
