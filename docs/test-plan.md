# Test Plan

## Automated Backend Tests

- Monitor state transitions: paused, up, degraded, down.
- Metrics calculations: uptime percentage, average latency, p95 latency, failures.
- Webhook payload shape.
- Future integration tests: auth flow, monitor CRUD, scheduled persistence, public status privacy, API key authentication, Flyway startup.

## Frontend Manual Checks

- Register and log in.
- Create a project and monitor.
- Run a manual check.
- Simulate a failing endpoint and confirm incident creation.
- Configure a webhook alert.
- Preview a public status page.
- Ask the AI assistant for an incident summary.
- Confirm an AI-suggested alert rule creation.
