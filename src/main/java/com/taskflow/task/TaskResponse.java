package com.taskflow.task;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Long projectId,
        String projectName,
        String assigneeUsername,
        String createdByUsername,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(),
                t.getProject().getId(), t.getProject().getName(),
                t.getAssignee() != null ? t.getAssignee().getUsername() : null,
                t.getCreatedBy().getUsername(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
