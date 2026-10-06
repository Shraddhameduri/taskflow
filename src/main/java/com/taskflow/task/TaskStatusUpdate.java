package com.taskflow.task;

import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdate(@NotNull TaskStatus status) {
}
