export type Role = "OWNER" | "ADMIN" | "MEMBER" | "VIEWER";
export type MonitorState = "UP" | "DEGRADED" | "DOWN" | "PAUSED";
export type MonitorMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE" | "HEAD";
export type AlertCondition = "MONITOR_DOWN" | "MONITOR_RECOVERED" | "LATENCY_THRESHOLD_EXCEEDED";

export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
}

export interface OrganizationSummary {
  id: string;
  name: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  user: UserProfile;
  organizations: OrganizationSummary[];
}

export interface Project {
  id: string;
  organizationId: string;
  name: string;
  slug: string;
  publicStatusEnabled: boolean;
  createdAt: string;
}

export interface Monitor {
  id: string;
  projectId: string;
  name: string;
  url: string;
  method: MonitorMethod;
  expectedStatusCode: number;
  timeoutSeconds: number;
  intervalSeconds: number;
  latencyThresholdMs: number;
  failureThreshold: number;
  consecutiveFailures: number;
  enabled: boolean;
  state: MonitorState;
  headers: Record<string, string>;
  body?: string;
  lastCheckedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MonitorRequest {
  name: string;
  url: string;
  method: MonitorMethod;
  expectedStatusCode: number;
  timeoutSeconds: number;
  intervalSeconds: number;
  latencyThresholdMs: number;
  failureThreshold: number;
  headers: Record<string, string>;
  body?: string;
  enabled: boolean;
}

export interface Check {
  id: string;
  monitorId: string;
  checkedAt: string;
  latencyMs?: number;
  statusCode?: number;
  success: boolean;
  errorCategory?: string;
  responseSnippet?: string;
}

export interface Metrics {
  monitorId: string;
  range: string;
  uptimePercentage: number;
  averageLatencyMs: number;
  p95LatencyMs: number;
  failureCount: number;
  incidentCount: number;
  recentChecks: Check[];
}

export interface IncidentEvent {
  id: string;
  type: string;
  message: string;
  metadataJson?: string;
  createdAt: string;
}

export interface Incident {
  id: string;
  projectId: string;
  monitorId: string;
  monitorName: string;
  title: string;
  status: "OPEN" | "RESOLVED";
  startedAt: string;
  resolvedAt?: string;
  events: IncidentEvent[];
}

export interface AlertRule {
  id: string;
  projectId: string;
  monitorId?: string;
  notificationChannelId?: string;
  name: string;
  condition: AlertCondition;
  thresholdMs?: number;
  enabled: boolean;
  createdAt: string;
}

export interface NotificationChannel {
  id: string;
  projectId: string;
  name: string;
  type: "WEBHOOK";
  webhookUrl: string;
  createdAt: string;
}

export interface StatusPage {
  projectId: string;
  projectName: string;
  slug: string;
  overallStatus: MonitorState;
  monitors: Array<{ id: string; name: string; state: MonitorState; lastLatencyMs?: number; lastCheckedAt?: string }>;
  activeIncidents: Array<{ id: string; title: string; monitorName: string; status: string; startedAt: string; resolvedAt?: string }>;
  recentResolvedIncidents: Array<{ id: string; title: string; monitorName: string; status: string; startedAt: string; resolvedAt?: string }>;
}

export interface ApiKey {
  id: string;
  organizationId: string;
  name: string;
  keyPrefix: string;
  createdAt: string;
  lastUsedAt?: string;
}

export interface AiResponse {
  message: string;
  toolsInvoked: string[];
  citations: string[];
  confirmationRequired: boolean;
  actionId?: string;
}
