package com.taskflow.user;

/**
 * Application roles. Stored on the user as {@code ROLE_<name>} authorities.
 *
 * <ul>
 *   <li>{@code ADMIN} — full access, including user management.</li>
 *   <li>{@code MANAGER} — manages projects and tasks, assigns work.</li>
 *   <li>{@code MEMBER} — works on tasks assigned to them.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    MANAGER,
    MEMBER
}
