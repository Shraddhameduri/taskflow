package com.taskflow.user;

import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        Set<Role> roles,
        boolean enabled) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRoles(), u.isEnabled());
    }
}
