package com.taskflow.project;

import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projects;
    private final UserRepository users;

    public ProjectService(ProjectRepository projects, UserRepository users) {
        this.projects = projects;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(Pageable pageable) {
        return projects.findAll(pageable).map(ProjectResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return ProjectResponse.from(findOrThrow(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProjectResponse create(ProjectRequest request, Authentication auth) {
        User owner = users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setOwner(owner);
        return ProjectResponse.from(projects.save(project));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findOrThrow(id);
        project.setName(request.name());
        project.setDescription(request.description());
        return ProjectResponse.from(projects.save(project));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void delete(Long id) {
        projects.delete(findOrThrow(id));
    }

    private Project findOrThrow(Long id) {
        return projects.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }
}
