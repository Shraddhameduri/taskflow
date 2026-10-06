package com.taskflow.task;

import com.taskflow.project.Project;
import com.taskflow.project.ProjectRepository;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import java.time.Instant;
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
public class TaskService {

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final UserRepository users;

    public TaskService(TaskRepository tasks, ProjectRepository projects, UserRepository users) {
        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listByProject(Long projectId, Pageable pageable) {
        projects.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        return tasks.findByProjectId(projectId, pageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> myTasks(Authentication auth, Pageable pageable) {
        return tasks.findByAssigneeUsername(auth.getName(), pageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(findOrThrow(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public TaskResponse create(TaskRequest request, Authentication auth) {
        Project project = projects.findById(request.projectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        User creator = users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setProject(project);
        task.setCreatedBy(creator);
        if (request.assigneeUsername() != null && !request.assigneeUsername().isBlank()) {
            task.setAssignee(users.findByUsername(request.assigneeUsername())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee not found")));
        }
        return TaskResponse.from(tasks.save(task));
    }

    @PreAuthorize("@taskSecurity.canModify(#id, authentication)")
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findOrThrow(id);
        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.assigneeUsername() != null) {
            task.setAssignee(request.assigneeUsername().isBlank() ? null
                    : users.findByUsername(request.assigneeUsername())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee not found")));
        }
        task.setUpdatedAt(Instant.now());
        return TaskResponse.from(tasks.save(task));
    }

    @PreAuthorize("@taskSecurity.canModify(#id, authentication)")
    public TaskResponse updateStatus(Long id, TaskStatusUpdate update) {
        Task task = findOrThrow(id);
        task.setStatus(update.status());
        task.setUpdatedAt(Instant.now());
        return TaskResponse.from(tasks.save(task));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void delete(Long id) {
        tasks.delete(findOrThrow(id));
    }

    private Task findOrThrow(Long id) {
        return tasks.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }
}
