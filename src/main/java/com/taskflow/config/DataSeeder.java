package com.taskflow.config;

import com.taskflow.project.Project;
import com.taskflow.project.ProjectRepository;
import com.taskflow.task.Task;
import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskRepository;
import com.taskflow.task.TaskStatus;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds demo accounts and sample data on first start (empty user table).
 *
 * <p>Demo credentials (change or disable in production):
 * <ul>
 *   <li>admin / admin123 — ADMIN</li>
 *   <li>manager / manager123 — MANAGER</li>
 *   <li>member / member123 — MEMBER</li>
 * </ul>
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UserRepository users, ProjectRepository projects,
                           TaskRepository tasks, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            User admin = user("admin", "admin@taskflow.local", "admin123",
                    Set.of(Role.ADMIN), encoder);
            User manager = user("manager", "manager@taskflow.local", "manager123",
                    Set.of(Role.MANAGER), encoder);
            User member = user("member", "member@taskflow.local", "member123",
                    Set.of(Role.MEMBER), encoder);
            users.save(admin);
            users.save(manager);
            users.save(member);

            Project project = new Project();
            project.setName("Website Redesign");
            project.setDescription("Q4 marketing site overhaul");
            project.setOwner(manager);
            projects.save(project);

            task(tasks, "Design new landing page", "Hero, pricing, testimonials",
                    TaskStatus.IN_PROGRESS, TaskPriority.HIGH, project, member, manager);
            task(tasks, "Migrate blog content", "Move 40 posts to the new CMS",
                    TaskStatus.TODO, TaskPriority.MEDIUM, project, member, manager);
            task(tasks, "Set up analytics", "GA4 + Search Console",
                    TaskStatus.DONE, TaskPriority.LOW, project, null, manager);
        };
    }

    private User user(String username, String email, String password, Set<Role> roles,
                      PasswordEncoder encoder) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setRoles(roles);
        return u;
    }

    private void task(TaskRepository tasks, String title, String description, TaskStatus status,
                      TaskPriority priority, Project project, User assignee, User createdBy) {
        Task t = new Task();
        t.setTitle(title);
        t.setDescription(description);
        t.setStatus(status);
        t.setPriority(priority);
        t.setProject(project);
        t.setAssignee(assignee);
        t.setCreatedBy(createdBy);
        tasks.save(t);
    }
}
