# Requirements traceability

This matrix records implemented behavior and executable evidence. It does not substitute planned stakeholder or usability evidence for results that do not yet exist.

| Requirement | Implementation | Automated evidence | Status |
| --- | --- | --- | --- |
| Authenticated student and admin access | Spring Security HTTP Basic with application-role filtering | `authenticationResponseContainsOnlyApplicationRoles`, unauthenticated endpoint tests | Complete for demo scope |
| Room and equipment catalogue | Active resource API and seeded `ROOM`/`EQUIPMENT` records | Authenticated resource API test | Complete |
| Conflict-safe booking | Future-time validation, per-resource pessimistic lock and overlap query | `overlappingBookingIsRejected` | Complete |
| Admin approval and rejection | Method-secured state transitions | Student forbidden tests, admin approve/reject tests | Complete |
| Owner/admin cancellation | Ownership guard and domain transition | Owner cancellation/rebooking and cross-owner denial tests | Complete |
| QR check-in | HTTPS QR link, login-preserved intent, explicit confirmation and secured API transition | Playwright `qr-check-in.spec.ts`; invalid and repeated check-in backend tests | Complete for online-first scope |
| PostgreSQL persistence | Flyway schema, production profile and Render/Neon configuration | CI PostgreSQL 17 job | Configured; public deployment pending |
| Responsive public demo | Vercel client plus separately hosted API | Production build and deployment manifests | Blocked until API deployment and Vercel environment update |
| Stakeholder validation | Scenario-based usability sessions | None | Not started; no result claimed |
