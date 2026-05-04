import type { AlertRule, ApiKey, Check, Incident, Metrics, Monitor, NotificationChannel, Project, StatusPage } from "./api/types";

export const demoProjects: Project[] = [
  {
    id: "11111111-1111-1111-1111-111111111111",
    organizationId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    name: "Production APIs",
    slug: "production-apis",
    publicStatusEnabled: true,
    createdAt: new Date().toISOString()
  }
];

export const demoChecks: Check[] = Array.from({ length: 18 }).map((_, index) => ({
  id: `check-${index}`,
  monitorId: "22222222-2222-2222-2222-222222222222",
  checkedAt: new Date(Date.now() - (17 - index) * 60 * 60 * 1000).toISOString(),
  latencyMs: [182, 190, 210, 320, 450, 820, 1240, 680, 390, 260, 220, 205, 198, 204, 230, 245, 212, 188][index],
  statusCode: index === 6 ? 503 : 200,
  success: index !== 6,
  errorCategory: index === 6 ? "UNEXPECTED_STATUS" : undefined,
  responseSnippet: index === 6 ? "503 service unavailable" : "ok"
}));

export const demoMonitors: Monitor[] = [
  {
    id: "22222222-2222-2222-2222-222222222222",
    projectId: demoProjects[0].id,
    name: "Billing API",
    url: "https://api.example.com/billing/health",
    method: "GET",
    expectedStatusCode: 200,
    timeoutSeconds: 5,
    intervalSeconds: 300,
    latencyThresholdMs: 750,
    failureThreshold: 3,
    consecutiveFailures: 0,
    enabled: true,
    state: "UP",
    headers: {},
    lastCheckedAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: "33333333-3333-3333-3333-333333333333",
    projectId: demoProjects[0].id,
    name: "Checkout API",
    url: "https://api.example.com/checkout/health",
    method: "GET",
    expectedStatusCode: 200,
    timeoutSeconds: 5,
    intervalSeconds: 300,
    latencyThresholdMs: 600,
    failureThreshold: 3,
    consecutiveFailures: 2,
    enabled: true,
    state: "DEGRADED",
    headers: {},
    lastCheckedAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }
];

export const demoMetrics: Metrics = {
  monitorId: demoMonitors[0].id,
  range: "24h",
  uptimePercentage: 94.4,
  averageLatencyMs: 367,
  p95LatencyMs: 1240,
  failureCount: 1,
  incidentCount: 1,
  recentChecks: demoChecks
};

export const demoIncidents: Incident[] = [
  {
    id: "44444444-4444-4444-4444-444444444444",
    projectId: demoProjects[0].id,
    monitorId: demoMonitors[1].id,
    monitorName: "Checkout API",
    title: "Checkout API latency degraded",
    status: "OPEN",
    startedAt: new Date(Date.now() - 45 * 60 * 1000).toISOString(),
    events: [
      { id: "event-1", type: "CREATED", message: "Latency threshold exceeded", createdAt: new Date(Date.now() - 45 * 60 * 1000).toISOString() },
      { id: "event-2", type: "ALERT_SENT", message: "Webhook delivered to Ops webhook", createdAt: new Date(Date.now() - 44 * 60 * 1000).toISOString() }
    ]
  }
];

export const demoChannels: NotificationChannel[] = [
  {
    id: "55555555-5555-5555-5555-555555555555",
    projectId: demoProjects[0].id,
    name: "Ops webhook",
    type: "WEBHOOK",
    webhookUrl: "https://hooks.example.com/api-sentinel",
    createdAt: new Date().toISOString()
  }
];

export const demoAlertRules: AlertRule[] = [
  {
    id: "66666666-6666-6666-6666-666666666666",
    projectId: demoProjects[0].id,
    monitorId: demoMonitors[1].id,
    notificationChannelId: demoChannels[0].id,
    name: "Checkout latency",
    condition: "LATENCY_THRESHOLD_EXCEEDED",
    thresholdMs: 600,
    enabled: true,
    createdAt: new Date().toISOString()
  }
];

export const demoStatusPage: StatusPage = {
  projectId: demoProjects[0].id,
  projectName: demoProjects[0].name,
  slug: demoProjects[0].slug,
  overallStatus: "DEGRADED",
  monitors: demoMonitors.map((monitor) => ({
    id: monitor.id,
    name: monitor.name,
    state: monitor.state,
    lastLatencyMs: monitor.id === demoMonitors[0].id ? 188 : 820,
    lastCheckedAt: monitor.lastCheckedAt
  })),
  activeIncidents: demoIncidents.map((incident) => ({
    id: incident.id,
    title: incident.title,
    monitorName: incident.monitorName,
    status: incident.status,
    startedAt: incident.startedAt
  })),
  recentResolvedIncidents: []
};

export const demoApiKeys: ApiKey[] = [
  {
    id: "77777777-7777-7777-7777-777777777777",
    organizationId: demoProjects[0].organizationId,
    name: "Local MCP client",
    keyPrefix: "aps_demo123",
    createdAt: new Date().toISOString()
  }
];
