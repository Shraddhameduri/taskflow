package com.taskflow.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List tasks of a project (paginated)")
    public Page<TaskResponse> listByProject(@PathVariable Long projectId,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return service.listByProject(projectId, pageable);
    }

    @GetMapping("/me")
    @Operation(summary = "List tasks assigned to the current user (paginated)")
    public Page<TaskResponse> myTasks(Authentication auth,
                                      @PageableDefault(size = 20) Pageable pageable) {
        return service.myTasks(auth, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by id")
    public TaskResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a task (ADMIN, MANAGER)")
    public TaskResponse create(@Valid @RequestBody TaskRequest request, Authentication auth) {
        return service.create(request, auth);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task (ADMIN, MANAGER, or the assignee)")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a task's status (ADMIN, MANAGER, or the assignee)")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusUpdate update) {
        return service.updateStatus(id, update);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task (ADMIN, MANAGER)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
