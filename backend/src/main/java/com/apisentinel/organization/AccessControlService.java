package com.apisentinel.organization;

import com.apisentinel.auth.CurrentUserService;
import com.apisentinel.common.ForbiddenException;
import com.apisentinel.common.NotFoundException;
import com.apisentinel.project.Project;
import com.apisentinel.project.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccessControlService {
    private final CurrentUserService currentUserService;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;

    public AccessControlService(
            CurrentUserService currentUserService,
            OrganizationMemberRepository memberRepository,
            OrganizationRepository organizationRepository,
            ProjectRepository projectRepository
    ) {
        this.currentUserService = currentUserService;
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
        this.projectRepository = projectRepository;
    }

    public Organization requireOrganization(UUID organizationId) {
        var user = currentUserService.currentUser();
        if (!memberRepository.existsByOrganizationIdAndUserId(organizationId, user.id())) {
            throw new ForbiddenException("You do not have access to this organization");
        }
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    public Project requireProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        requireOrganization(project.getOrganization().getId());
        return project;
    }

    public OrganizationMember requireWritableOrganization(UUID organizationId) {
        var user = currentUserService.currentUser();
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, user.id())
                .orElseThrow(() -> new ForbiddenException("You do not have access to this organization"));
        if (member.getRole() == OrganizationRole.VIEWER) {
            throw new ForbiddenException("Viewer role cannot modify this organization");
        }
        return member;
    }
}
