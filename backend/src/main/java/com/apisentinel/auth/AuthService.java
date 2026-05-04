package com.apisentinel.auth;

import com.apisentinel.common.BadRequestException;
import com.apisentinel.common.NotFoundException;
import com.apisentinel.common.SlugUtil;
import com.apisentinel.organization.Organization;
import com.apisentinel.organization.OrganizationMember;
import com.apisentinel.organization.OrganizationMemberRepository;
import com.apisentinel.organization.OrganizationRepository;
import com.apisentinel.organization.OrganizationRole;
import com.apisentinel.organization.UserAccount;
import com.apisentinel.organization.UserRepository;
import com.apisentinel.project.Project;
import com.apisentinel.project.ProjectRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository memberRepository,
            ProjectRepository projectRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }
        UserAccount user = userRepository.save(new UserAccount(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        ));
        Organization organization = organizationRepository.save(new Organization(request.organizationName().trim()));
        memberRepository.save(new OrganizationMember(organization, user, OrganizationRole.OWNER));
        projectRepository.save(new Project(organization, "Production APIs", SlugUtil.from(request.organizationName())));
        return responseFor(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserAccount user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return responseFor(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse me(AuthenticatedUser currentUser) {
        UserAccount user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return responseFor(user);
    }

    private AuthDtos.AuthResponse responseFor(UserAccount user) {
        List<AuthDtos.OrganizationSummary> organizations = memberRepository.findAllByUserId(user.getId()).stream()
                .map(member -> new AuthDtos.OrganizationSummary(
                        member.getOrganization().getId(),
                        member.getOrganization().getName(),
                        member.getRole()
                ))
                .toList();
        return new AuthDtos.AuthResponse(
                jwtService.issue(user.getId(), user.getEmail()),
                new AuthDtos.UserProfile(user.getId(), user.getEmail(), user.getDisplayName()),
                organizations
        );
    }
}
