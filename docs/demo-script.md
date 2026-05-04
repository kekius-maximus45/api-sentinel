# Demo Script

Use this walkthrough for a recruiter, GitHub video, or live portfolio demo.

## Setup

Start the backend without Docker:

```powershell
cd E:\PROJECTS\api-sentinel\backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Start the frontend:

```powershell
cd E:\PROJECTS\api-sentinel\frontend
npm run dev -- --host 127.0.0.1
```

Start a test API target:

```powershell
cd E:\PROJECTS\express-app
npm start
```

Test target health URL:

```text
http://localhost:5000/api/health
```

## Walkthrough

1. Register a new account.
2. Open **Monitors** and create `Express App Health`.
3. Use URL `http://localhost:5000/api/health`, method `GET`, expected status `200`, interval `60`, timeout `5`.
4. Open the monitor detail page and click **Run check**.
5. Show the successful check, latency, and `UP` state.
6. Stop the Express app.
7. Click **Run check** three times to cross the failure threshold.
8. Show the monitor state moving to `DOWN`.
9. Open **Incidents** and show the automatically opened incident and timeline events.
10. Restart the Express app and run one more check.
11. Show the monitor returning to `UP`.
12. Open **Dashboard** and explain uptime, p95 latency, failures, and active incident count.
13. Open **Status Page** and show customer-safe public health.
14. Open **AI Assistant**, select the monitor, and ask: `Summarize what happened with Express App Health.`
15. Open **API Keys**, create a key, and explain that MCP tools use hashed one-time API keys.

## Talk Track

API Sentinel is an API monitoring SaaS. It checks configured HTTP endpoints on a schedule, stores every check result, calculates uptime and latency metrics, opens incidents after repeated failures, sends webhook alerts, exposes public status pages, and provides an MCP/AI assistant for incident summaries and status updates.
