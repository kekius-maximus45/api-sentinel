package com.apisentinel.ai;

import com.apisentinel.organization.Organization;
import com.apisentinel.organization.UserAccount;
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
@Table(name = "ai_audit_logs")
public class AiAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false)
    private String promptSummary;

    @Column(columnDefinition = "text")
    private String toolsInvoked;

    @Column(nullable = false)
    private String resultStatus;

    @Column(nullable = false)
    private Instant createdAt;

    protected AiAuditLog() {
    }

    public AiAuditLog(UserAccount user, Organization organization, String promptSummary, String toolsInvoked, String resultStatus) {
        this.user = user;
        this.organization = organization;
        this.promptSummary = promptSummary;
        this.toolsInvoked = toolsInvoked;
        this.resultStatus = resultStatus;
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

    public UserAccount getUser() {
        return user;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getPromptSummary() {
        return promptSummary;
    }

    public String getToolsInvoked() {
        return toolsInvoked;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
