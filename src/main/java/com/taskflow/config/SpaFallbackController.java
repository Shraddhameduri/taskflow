package com.taskflow.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards Angular SPA routes to index.html so that deep links and
 * browser refreshes work with PathLocationStrategy. API, actuator,
 * and Swagger paths are unaffected (they are listed explicitly here,
 * never matched by these patterns).
 */
@Controller
public class SpaFallbackController {

    @GetMapping({
            "/login",
            "/register",
            "/dashboard",
            "/my-tasks",
            "/admin",
            "/projects",
            "/projects/**"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
