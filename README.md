# TaskFlow

![CI](https://github.com/Shraddhameduri/taskflow/actions/workflows/ci.yml/badge.svg)

A full end-to-end team task management application built with **Java 17 + Spring Boot 3**, featuring **JWT authentication** and **role-based access control (RBAC)** — REST API, web UI, tests, Docker, and CI included.

## Features

- **Auth** — register / login with BCrypt-hashed passwords, short-lived JWT access tokens + rotating refresh tokens (server-side revocation, logout / logout-everywhere)
- **RBAC** — three roles enforced at the URL level *and* the method level (`@PreAuthorize`), plus ownership checks (members can only touch tasks assigned to them)
- **Brute-force protection** — Bucket4j rate limiting on login/register (10 attempts/min/IP → `429`)
- **Database migrations** — Flyway versioned schema (works on H2 and PostgreSQL)
- **Domain** — users, projects, tasks (status / priority / assignee), all list endpoints paginated
- **Web UI** — Angular 22 single-page app served by Spring Boot (no separate deployment): login/register, project dashboard, kanban task board, "my tasks", role-aware admin panel, silent token refresh with an HTTP interceptor
- **API docs** — Swagger UI at `/swagger-ui.html`
- **Observability** — Spring Boot Actuator (`/actuator/health`, `/actuator/info`)
- **Tests** — 13 MockMvc integration tests covering auth, rotation, logout, rate limiting, and the full RBAC matrix
- **Docker** — multi-stage `Dockerfile` + `docker-compose.yml` (app + PostgreSQL)
- **CI** — GitHub Actions: build, test, and Docker image build on every push/PR

## RBAC matrix

| Capability | ADMIN | MANAGER | MEMBER |
|---|---|---|---|
| Register / log in | ✓ | ✓ | ✓ |
| View projects & tasks | ✓ | ✓ | ✓ |
| Create / edit / delete projects | ✓ | ✓ | ✗ |
| Create / delete tasks | ✓ | ✓ | ✗ |
| Edit task details | ✓ | ✓ | own tasks only |
| Change task status | ✓ | ✓ | own tasks only |
| Manage users (roles, enable/disable) | ✓ | ✗ | ✗ |

## Quickstart

**Prerequisites:** Java 17+, Maven 3.9+ (or use Docker).

```bash
# Run with the embedded H2 database (zero setup; Flyway migrates the schema)
mvn spring-boot:run
```

Open http://localhost:8080 — log in with a demo account:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `manager` | `manager123` | MANAGER |
| `member` | `member123` | MEMBER |

> Demo data is seeded only on first start (empty database). Change or remove the demo credentials for production.

**With PostgreSQL via Docker:**

```bash
docker compose up --build
```

This starts PostgreSQL 16 and the app with the `postgres` Spring profile.

**Run the tests:**

```bash
mvn test
```

**Frontend development:**

```bash
cd frontend
npm install
npm start   # ng serve on :4200, /api proxied to localhost:8080
```

The production build is wired into Maven via `frontend-maven-plugin`: `mvn package`
installs Node, runs `npm install && npm run build`, and the Angular output is served
from the same jar (deep links like `/dashboard` are forwarded to `index.html`).
No frontend build step is needed to run the app.

## API reference

All `/api/**` endpoints except `/api/auth/**` require `Authorization: Bearer <access-token>`.
List endpoints are paginated: `?page=0&size=20&sort=createdAt,desc`.

### Auth

| Method | Endpoint | Body | Description |
|---|---|---|---|
| POST | `/api/auth/register` | `{username, email, password}` | Register (gets MEMBER role) → tokens |
| POST | `/api/auth/login` | `{username, password}` | Log in → tokens |
| POST | `/api/auth/refresh` | `{refreshToken}` | Rotate: new pair, old token revoked |
| POST | `/api/auth/logout` | `{refreshToken}` | Revoke one refresh token |
| POST | `/api/auth/logout-all` | — | Revoke all of the caller's refresh tokens |

Login/register are rate-limited (10 req/min per IP → `429 Too Many Requests`).

### Projects

| Method | Endpoint | Roles |
|---|---|---|
| GET | `/api/projects`, `/api/projects/{id}` | any authenticated user |
| POST | `/api/projects` | ADMIN, MANAGER |
| PUT | `/api/projects/{id}` | ADMIN, MANAGER |
| DELETE | `/api/projects/{id}` | ADMIN, MANAGER |

### Tasks

| Method | Endpoint | Roles |
|---|---|---|
| GET | `/api/tasks/project/{projectId}`, `/api/tasks/me`, `/api/tasks/{id}` | any authenticated user |
| POST | `/api/tasks` | ADMIN, MANAGER |
| PUT | `/api/tasks/{id}` | ADMIN, MANAGER, or assignee |
| PATCH | `/api/tasks/{id}/status` | ADMIN, MANAGER, or assignee |
| DELETE | `/api/tasks/{id}` | ADMIN, MANAGER |

### Users

| Method | Endpoint | Roles |
|---|---|---|
| GET | `/api/users/me` | any authenticated user |
| GET | `/api/users`, `/api/users/{id}` | ADMIN |
| PUT | `/api/users/{id}/roles` | ADMIN |
| PATCH | `/api/users/{id}/enabled` | ADMIN |
| DELETE | `/api/users/{id}` | ADMIN |

### Example

```bash
# Log in
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"manager","password":"manager123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")

# Create a project
curl -X POST localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Launch v2","description":"Q1 release"}'

# A member trying the same gets 403 Forbidden
```

## Configuration

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | dev-only key | HMAC secret for JWT signing (min 32 bytes) — **required in production**; startup fails if unset on the `postgres`/`prod` profiles |
| `SPRING_PROFILES_ACTIVE` | — | `postgres` for the PostgreSQL profile |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | H2 in-memory | Database connection |
| `APP_CORS_ORIGINS` | same-origin | Comma-separated extra origins allowed to call `/api/**` |

## Project structure

```
src/main/java/com/taskflow/
├── TaskflowApplication.java
├── auth/        # register/login/refresh/logout + rotation, RefreshToken entity
├── common/      # GlobalExceptionHandler (uniform JSON errors)
├── config/      # SecurityConfig, DataSeeder, OpenApiConfig, JwtSecretValidator, SpaFallbackController
├── project/     # Project entity, service (@PreAuthorize), paginated controller
├── security/    # JwtService, JwtAuthenticationFilter, AuthRateLimitFilter (Bucket4j),
│                # CustomUserDetailsService
├── task/        # Task entity, TaskService, TaskSecurity ownership bean, controller
├── user/        # User entity/roles, admin UserService/Controller, /users/me
├── user/        # User entity/roles, admin UserService/Controller, /users/me
└── resources/
    ├── application.yml        # H2 default + postgres profile, actuator
    ├── db/migration/          # Flyway versioned schema (V1__initial_schema.sql)
    └── static/                # Angular build output (generated; git-ignored)
frontend/                      # Angular 22 SPA source
├── src/app/core/              # models, AuthService, JWT interceptor, guards, API services
├── src/app/features/          # login, register, dashboard, project board, my-tasks, admin
├── src/app/layout/            # app shell (sidebar nav + user menu)
└── flatten-output.js          # flattens ng build output for Spring Boot's static/
src/test/java/com/taskflow/
├── AuthIntegrationTest.java   # register/login/rotation/logout/duplicate/unauthorized
├── RbacIntegrationTest.java   # full RBAC matrix against demo accounts
└── RateLimitIntegrationTest.java # 429 after 10 rapid logins
.github/workflows/ci.yml       # build + test + docker image on push/PR
```

## Security notes

- Passwords are BCrypt-hashed; JWTs are HMAC-SHA signed with a startup-validated secret and short expiry; every token carries a unique `jti`
- Refresh tokens rotate on each use and only SHA-256 hashes are stored — reuse of a rotated token fails closed
- Stateless sessions, CSRF disabled for the token API, HSTS + `nosniff` headers, explicit CORS
- Method security complements URL rules so authorization can't be bypassed by guessing paths
- Uniform JSON error responses (no stack traces leak to clients)
