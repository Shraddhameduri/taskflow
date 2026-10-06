package com.taskflow.task;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Ownership checks used by {@code @PreAuthorize} expressions.
 * A task can be modified by an ADMIN, a MANAGER, or the MEMBER it is assigned to.
 */
@Component("taskSecurity")
public class TaskSecurity {

    private final TaskRepository tasks;

    public TaskSecurity(TaskRepository tasks) {
        this.tasks = tasks;
    }

    public boolean canModify(Long taskId, Authentication auth) {
        if (auth == null) {
            return false;
        }
        boolean privileged = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"));
        if (privileged) {
            return true;
        }
        return tasks.findById(taskId)
                .map(t -> t.getAssignee() != null && t.getAssignee().getUsername().equals(auth.getName()))
                .orElse(false);
    }
}
