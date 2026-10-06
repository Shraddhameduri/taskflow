package com.taskflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the RBAC matrix with the seeded demo accounts:
 * admin/admin123 (ADMIN), manager/manager123 (MANAGER), member/member123 (MEMBER).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class RbacIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private String token(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void adminCanListUsers_butMemberCannot() throws Exception {
        String admin = token("admin", "admin123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").exists());

        String member = token("member", "member123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + member))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreateProject_butMemberCannot() throws Exception {
        String manager = token("manager", "manager123");
        mvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "RBAC test project", "description", "created by test"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("RBAC test project"));

        String member = token("member", "member123");
        mvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "nope"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCannotDeleteProject() throws Exception {
        String member = token("member", "member123");
        mvc.perform(delete("/api/projects/1").header("Authorization", "Bearer " + member))
                .andExpect(status().isForbidden());
    }

    @Test
    void assigneeCanUpdateOwnTaskStatus_butNotOthers() throws Exception {
        String member = token("member", "member123");
        String manager = token("manager", "manager123");

        // Seeded task 1 ("Design new landing page") is assigned to member.
        mvc.perform(patch("/api/tasks/1/status")
                        .header("Authorization", "Bearer " + member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // Create a task assigned to manager; member must not touch it.
        String created = mvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "title", "Manager-only task",
                                "projectId", 1,
                                "assigneeUsername", "manager"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(created);

        mvc.perform(patch("/api/tasks/" + node.get("id").asLong() + "/status")
                        .header("Authorization", "Bearer " + member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isForbidden());

        // Manager can update anyone's task.
        mvc.perform(patch("/api/tasks/" + node.get("id").asLong() + "/status")
                        .header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void memberCannotDeleteTask() throws Exception {
        String member = token("member", "member123");
        mvc.perform(delete("/api/tasks/1").header("Authorization", "Bearer " + member))
                .andExpect(status().isForbidden());
    }
}
