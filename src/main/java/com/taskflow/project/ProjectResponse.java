package com.taskflow.project;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String ownerUsername,
        Instant createdAt) {

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getOwner().getUsername(), p.getCreatedAt());
    }
}
