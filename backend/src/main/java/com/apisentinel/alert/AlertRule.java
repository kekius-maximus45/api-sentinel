package com.apisentinel.alert;

import com.apisentinel.monitor.Monitor;
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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_rules")
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id")
    private Monitor monitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_channel_id")
    private NotificationChannel notificationChannel;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertCondition condition;

    private Integer thresholdMs;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Instant createdAt;

    protected AlertRule() {
    }

    public AlertRule(Project project, Monitor monitor, NotificationChannel notificationChannel, String name,
                     AlertCondition condition, Integer thresholdMs, boolean enabled) {
        this.project = project;
        this.monitor = monitor;
        this.notificationChannel = notificationChannel;
        this.name = name;
        this.condition = condition;
        this.thresholdMs = thresholdMs;
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void update(Monitor monitor, NotificationChannel notificationChannel, String name, AlertCondition condition,
                       Integer thresholdMs, boolean enabled) {
        this.monitor = monitor;
        this.notificationChannel = notificationChannel;
        this.name = name;
        this.condition = condition;
        this.thresholdMs = thresholdMs;
        this.enabled = enabled;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public NotificationChannel getNotificationChannel() {
        return notificationChannel;
    }

    public String getName() {
        return name;
    }

    public AlertCondition getCondition() {
        return condition;
    }

    public Integer getThresholdMs() {
        return thresholdMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
