package com.taskflow.config;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails fast at startup when the JWT signing secret is weak or still the
 * development default in a production profile.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtSecretValidator implements ApplicationRunner {

    private static final String DEV_DEFAULT = "taskflow-dev-secret-key-must-be-at-least-32-chars-long";

    private final String secret;
    private final Environment env;

    public JwtSecretValidator(@Value("${app.jwt.secret}") String secret, Environment env) {
        this.secret = secret;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean production = env.matchesProfiles("prod", "postgres", "production");
        int bytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes for HS256 (got " + bytes + ")");
        }
        if (production && DEV_DEFAULT.equals(secret)) {
            throw new IllegalStateException(
                    "Refusing to start: app.jwt.secret is still the development default. "
                            + "Set the JWT_SECRET environment variable.");
        }
    }
}
