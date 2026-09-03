# DegreeFlow requirements traceability

This matrix maps each requirement to reachable implementation and executable evidence. It does not substitute planned stakeholder or usability work for results that do not yet exist.

| Requirement | Reachable implementation | Verification evidence | Status |
| --- | --- | --- | --- |
| Student and reviewer authentication | Spring Security HTTP Basic; login calls `/api/auth/me` | authentication and unauthenticated-request integration tests | Complete for demo scope |
| Student subject management | Subject form and list call student-only create, update and archive endpoints | subject lifecycle integration test; Playwright student workflow | Complete |
| Subject and goal owner isolation | Repository lookups include the authenticated owner | owner-scoped and cross-owner denial tests | Complete |
| Four required goal types | Assignment, Study Session, Project Milestone and Portfolio in API and UI | create/update integration test; Playwright milestone workflow | Complete |
| Required goal fields and timestamps | Entity, V2 schema, request validation and UI form/list | backend create/update assertions; Playwright payload assertions | Complete |
| Legal goal workflow | Domain transition map for TODO, IN_PROGRESS, COMPLETED and CANCELLED | legal transition integration test | Complete |
| Terminal-state protection | Completed/cancelled entities reject edits and further transitions | terminal-state integration test | Complete |
| Study-session time validation | Planned start required and due time must be later | time-range validation integration test | Complete |
| Study-session overlap prevention | Per-student workspace lock plus active-session overlap query in one transaction | overlap rejection and non-study control tests | Complete |
| Optimistic locking | `@Version` fields and columns on subjects and goals | clean schema validation; PostgreSQL CI job | Complete |
| Evidence and privacy | URL validation; visibility limited to project/portfolio; reviewer query filters shared records | evidence validation and reviewer visibility tests | Complete |
| Reviewer read-only access | reviewer can only read shared goals; subject and write endpoints are denied | backend reviewer tests; Playwright reviewer workflow | Complete |
| Seven dashboard measures | computed metrics plus subject open-goal counts | Playwright desktop/mobile rendering and workflow checks | Complete |
| Consistent API errors | central handler for validation, conflict, access, optimistic-lock and not-found failures | status/message assertions across integration suite | Complete |
| Responsive UI | desktop Chromium and Pixel 7 projects in Playwright | four browser tests | Complete |
| OpenAPI and health | Springdoc Swagger UI and Actuator health endpoint | application context test; deployment health check required | Implemented; online recheck pending |
| Production migration | immutable historical V1, V2 DegreeFlow schema and V3 obsolete-table removal | H2 Flyway migration; PostgreSQL CI and Neon checks required | Local complete; hosted recheck pending |
| CI and containers | GitHub Actions runs H2, PostgreSQL, frontend and image jobs | new revision CI run required | Configured; remote run pending |
| Public full-stack deployment | Vercel client, Render API and Neon database | dated Student/Reviewer online workflow required | Pending this revision |
| Stakeholder validation | scenario-based sessions with real users | None | Not started; no result claimed |

## Submission boundary

The V1 file remains only because applied Flyway migrations are immutable. V3 removes its obsolete tables, and no DegreeFlow controller, service, repository, test or UI imports the old domain. Old interface screenshots are not DegreeFlow evidence.
