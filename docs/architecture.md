# Architecture Notes

API Sentinel is intentionally a modular monolith. Each package owns one domain boundary while sharing the same PostgreSQL transaction model:

- `auth`: JWT security, registration, login, current user.
- `organization`: users, organizations, roles, membership checks.
- `project`: project CRUD and public slug ownership.
- `monitor`: monitor configuration and metrics APIs.
- `check`: async HTTP check execution and scheduler.
- `incident`: incident lifecycle and timeline events.
- `alert`: alert rules, webhook channels, payload creation.
- `statuspage`: privacy-safe public status projections.
- `ai`: incident-assistant REST workflow and audit logging.
- `mcp`: Spring AI MCP tool surface with API-key authorization.
- `apikey`: one-time API key creation and hashed key lookup.

The scheduler scans enabled monitors every few seconds and dispatches due checks asynchronously. Each check records latency, status code, success, error category, and a short response snippet. The state machine maps the latest result and consecutive failures into `UP`, `DEGRADED`, `DOWN`, or `PAUSED`.

Public status pages never expose monitor URLs, headers, request bodies, API keys, or raw response snippets.
