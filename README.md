# Campus Reserve

Campus Reserve is a full-stack portfolio project for booking campus rooms and equipment. It demonstrates a complete vertical slice rather than a static UI: authenticated users can browse resources, submit bookings, receive conflict feedback, complete an approval workflow, and check in with a generated QR code.

## What works

- Student and administrator demo roles protected by Spring Security.
- Active room and equipment catalogue.
- Future-dated booking requests with validation.
- Transactional overlap prevention for each resource.
- Admin approval and rejection.
- Owner/admin cancellation.
- Approved-booking QR and check-in code.
- Flyway database migration for H2 locally and PostgreSQL in Docker.
- OpenAPI JSON and Swagger UI.
- Ten passing Spring Boot tests covering authentication, authorization, booking conflicts, approval, rejection and check-in failures.
- A production React build deployed on Vercel.

## Deployment status

- Vercel frontend: <https://campus-resource-booking-eta.vercel.app>
- Spring Boot API: local only; an external Java host and PostgreSQL database are still required for the public login and booking workflow.

Vercel is intentionally used for the React application. The Java API remains a separate deployable service because Java is not an official Vercel Functions runtime. The production deployment is prepared for a Render web service backed by a Neon PostgreSQL database; `render.yaml` defines the API service and prompts for the database connection string without committing it. Set `VITE_API_URL` in Vercel to the deployed API URL ending in `/api`, then redeploy the frontend.

## Production deployment

1. Provision a Neon PostgreSQL database in the Singapore region and copy its pooled connection string.
2. Create a Render Blueprint from this repository. `render.yaml` creates the free Docker web service and prompts for `DATABASE_URL` without storing it in Git.
3. Wait for `/actuator/health` on the Render service to return `{"status":"UP"}`. Flyway creates and seeds the schema during startup.
4. Add `VITE_API_URL=https://<render-service>.onrender.com/api` to the Vercel production environment and redeploy the frontend.
5. Verify both demo roles, booking creation, overlap rejection, admin approval and QR check-in against the public URLs.

Free Render web services can cold-start after inactivity. The database is kept separately on Neon so it does not inherit Render's 30-day free-database expiry.

## Architecture

```mermaid
flowchart LR
    Browser[React + TypeScript] -->|REST + Basic Auth| API[Spring Boot API]
    API --> Security[Spring Security RBAC]
    API --> Service[Transactional booking service]
    Service --> DB[(PostgreSQL)]
    Flyway[Flyway migrations] --> DB
```

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> APPROVED
    PENDING --> REJECTED
    PENDING --> CANCELLED
    APPROVED --> CHECKED_IN
    APPROVED --> CANCELLED
```

## Run locally without Docker

Requirements: Java 21 and Node.js 22.

Terminal 1:

```bash
cd backend
./mvnw spring-boot:run
```

Terminal 2:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`.

Demo users:

| Role | Email | Password |
| --- | --- | --- |
| Student | `student@campus.local` | `Student123!` |
| Admin | `admin@campus.local` | `Admin123!` |

The local backend uses an in-memory H2 database in PostgreSQL compatibility mode. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Run the PostgreSQL stack

```bash
docker compose up --build
```

Then open `http://localhost:5173`. The database is persisted in the `campus_postgres` Docker volume.

## Verify

```bash
cd backend
./mvnw test

cd ../frontend
npm run lint
npm run build
```

CI repeats these checks, runs the backend suite against PostgreSQL 17, and builds both Docker images.

## Engineering decisions

- A pessimistic lock serializes competing requests for the same resource before the overlap check. `@Version` provides optimistic protection when an existing booking changes state.
- Database structure is owned by a Flyway migration; Hibernate validates rather than silently changing production schema.
- Open EntityManager in View is disabled. Repository entity graphs load the resource information required by API responses.
- Credentials remain in React memory. HTTP Basic is intentionally limited to the local vertical slice and must run behind HTTPS. Before a public production launch, replace demo users with database-backed identities and secure HttpOnly session cookies or an external identity provider.
- QR codes encode only a booking identifier and one-time-style check-in code. They contain no student profile data.

## Next milestones

1. Replace in-memory users with database-backed accounts and password reset.
2. Add Playwright end-to-end coverage for student booking and admin approval.
3. Add email notifications and expiring check-in codes.
4. Run stakeholder usability sessions and record task-completion evidence.
5. Publish a live demo with seeded, non-personal data.

## Scope boundary

This project contains no AI or machine learning. It exists to demonstrate software engineering fundamentals: API design, authorization, data integrity, responsive UI, migrations, automated tests and deployment packaging.
