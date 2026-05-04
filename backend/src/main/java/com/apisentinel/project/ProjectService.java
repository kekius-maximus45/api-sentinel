package com.apisentinel.project;

import com.apisentinel.auth.CurrentUserService;
import com.apisentinel.common.NotFoundException;
import com.apisentinel.common.SlugUtil;
import com.apisentinel.organization.AccessControlService;
import com.apisentinel.organization.OrganizationMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {
    private final CurrentUserService currentUserService;
    private final OrganizationMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final AccessControlService accessControlService;

    public ProjectService(
            CurrentUserService currentUserService,
            OrganizationMemberRepository memberRepository,
            ProjectRepository projectRepository,
            AccessControlService accessControlService
    ) {
        this.currentUserService = currentUserService;
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
        this.accessControlService = accessControlService;
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectResponse> list() {
        var user = currentUserService.currentUser();
        return memberRepository.findAllByUserId(user.id()).stream()
                .flatMap(member -> projectRepository.findAllByOrganizationId(member.getOrganization().getId()).stream())
                .sorted(Comparator.comparing(Project::getCreatedAt))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectDtos.ProjectResponse create(ProjectDtos.ProjectRequest request) {
        var organization = accessControlService.requireWritableOrganization(request.organizationId()).getOrganization();
        Project project = projectRepository.save(new Project(organization, request.name().trim(), SlugUtil.from(request.name())));
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectDtos.ProjectResponse get(UUID projectId) {
        return toResponse(accessControlService.requireProject(projectId));
    }

    public Project requirePublicProject(String slug) {
        Project project = projectRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Status page not found"));
        if (!project.isPublicStatusEnabled()) {
            throw new NotFoundException("Status page not found");
        }
        return project;
    }

    private ProjectDtos.ProjectResponse toResponse(Project project) {
        return new ProjectDtos.ProjectResponse(
                project.getId(),
                project.getOrganization().getId(),
                project.getName(),
                project.getSlug(),
                project.isPublicStatusEnabled(),
                project.getCreatedAt()
        );
    }
}
