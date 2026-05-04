import { useQuery } from "@tanstack/react-query";
import { Activity, AlertTriangle, Clock3, Gauge } from "lucide-react";
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, withFallback } from "../api/client";
import type { Incident, Metrics, Monitor } from "../api/types";
import { MetricCard } from "../components/MetricCard";
import { StatusBadge } from "../components/StatusBadge";
import { useProjects } from "../ProjectContext";
import { demoIncidents, demoMetrics, demoMonitors } from "../mockData";

export function DashboardPage() {
  const { selectedProject } = useProjects();
  const projectId = selectedProject?.id;
  const monitorsQuery = useQuery({
    queryKey: ["monitors", projectId],
    enabled: Boolean(projectId),
    queryFn: () => withFallback(api.get<Monitor[]>(`/api/projects/${projectId}/monitors`), demoMonitors)
  });
  const monitors = monitorsQuery.data ?? demoMonitors;
  const primaryMonitor = monitors[0] ?? demoMonitors[0];
  const metricsQuery = useQuery({
    queryKey: ["metrics", primaryMonitor?.id, "24h"],
    enabled: Boolean(primaryMonitor?.id),
    queryFn: () => withFallback(api.get<Metrics>(`/api/monitors/${primaryMonitor.id}/metrics?range=24h`), demoMetrics)
  });
  const incidentsQuery = useQuery({
    queryKey: ["incidents", projectId],
    enabled: Boolean(projectId),
    queryFn: () => withFallback(api.get<Incident[]>(`/api/projects/${projectId}/incidents`), demoIncidents)
  });
  const metrics = metricsQuery.data ?? demoMetrics;
  const incidents = incidentsQuery.data ?? demoIncidents;
  const activeIncidents = incidents.filter((incident) => incident.status === "OPEN").length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">Dashboard</h1>
        <p className="mt-1 text-sm text-slate-500">{selectedProject?.name} health summary</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard icon={Gauge} label="Uptime" value={`${metrics.uptimePercentage.toFixed(1)}%`} detail="Last 24 hours" />
        <MetricCard icon={Clock3} label="P95 latency" value={`${Math.round(metrics.p95LatencyMs)} ms`} detail={primaryMonitor.name} />
        <MetricCard icon={AlertTriangle} label="Failures" value={String(metrics.failureCount)} detail="Recorded checks" />
        <MetricCard icon={Activity} label="Active incidents" value={String(activeIncidents)} detail={`${monitors.length} monitors`} />
      </div>

      <section className="grid gap-4 xl:grid-cols-[1fr_380px]">
        <div className="panel p-4">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="section-title">Latency trend</h2>
            <StatusBadge state={primaryMonitor.state} />
          </div>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={metrics.recentChecks.map((check) => ({ time: new Date(check.checkedAt).getHours() + ":00", latency: check.latencyMs ?? 0 }))}>
                <XAxis dataKey="time" stroke="#64748b" fontSize={12} />
                <YAxis stroke="#64748b" fontSize={12} />
                <Tooltip />
                <Line type="monotone" dataKey="latency" stroke="#2563eb" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="panel p-4">
          <h2 className="section-title">Monitor states</h2>
          <div className="mt-4 divide-y divide-line">
            {monitors.map((monitor) => (
              <div key={monitor.id} className="flex items-center justify-between py-3">
                <div>
                  <div className="text-sm font-semibold">{monitor.name}</div>
                  <div className="text-xs text-slate-500">{monitor.method} {monitor.expectedStatusCode}</div>
                </div>
                <StatusBadge state={monitor.state} />
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
