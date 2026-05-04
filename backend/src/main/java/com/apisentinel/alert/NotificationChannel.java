package com.apisentinel.alert;

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
@Table(name = "notification_channels")
public class NotificationChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannelType type;

    @Column(nullable = false, columnDefinition = "text")
    private String webhookUrl;

    @Column(nullable = false)
    private Instant createdAt;

    protected NotificationChannel() {
    }

    public NotificationChannel(Project project, String name, NotificationChannelType type, String webhookUrl) {
        this.project = project;
        this.name = name;
        this.type = type;
        this.webhookUrl = webhookUrl;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
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

    public NotificationChannelType getType() {
        return type;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
