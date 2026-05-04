import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Pause, Play, RefreshCw } from "lucide-react";
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, withFallback } from "../api/client";
import type { Check, Incident, Metrics, Monitor } from "../api/types";
import { StatusBadge } from "../components/StatusBadge";
import { demoChecks, demoIncidents, demoMetrics, demoMonitors } from "../mockData";
import { useProjects } from "../ProjectContext";

export function MonitorDetailPage() {
  const { monitorId } = useParams();
  const { selectedProject } = useProjects();
  const queryClient = useQueryClient();
  const fallbackMonitor = demoMonitors.find((monitor) => monitor.id === monitorId) ?? demoMonitors[0];
  const monitorQuery = useQuery({
    queryKey: ["monitor", monitorId],
    enabled: Boolean(monitorId),
    queryFn: () => withFallback(api.get<Monitor>(`/api/monitors/${monitorId}`), fallbackMonitor)
  });
  const metricsQuery = useQuery({
    queryKey: ["metrics", monitorId, "24h"],
    enabled: Boolean(monitorId),
    queryFn: () => withFallback(api.get<Metrics>(`/api/monitors/${monitorId}/metrics?range=24h`), { ...demoMetrics, monitorId: monitorId ?? demoMetrics.monitorId })
  });
  const checksQuery = useQuery({
    queryKey: ["checks", monitorId],
    enabled: Boolean(monitorId),
    queryFn: () => withFallback(api.get<Check[]>(`/api/monitors/${monitorId}/checks`), demoChecks)
  });
  const incidentsQuery = useQuery({
    queryKey: ["incidents", selectedProject?.id],
    enabled: Boolean(selectedProject?.id),
    queryFn: () => withFallback(api.get<Incident[]>(`/api/projects/${selectedProject?.id}/incidents`), demoIncidents)
  });
  const runMutation = useMutation({
    mutationFn: () => api.post(`/api/monitors/${monitorId}/run-check`),
    onSuccess: invalidate
  });
  const pauseMutation = useMutation({
    mutationFn: () => api.post(`/api/monitors/${monitorId}/pause`),
    onSuccess: invalidate
  });
  const resumeMutation = useMutation({
    mutationFn: () => api.post(`/api/monitors/${monitorId}/resume`),
    onSuccess: invalidate
  });

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ["monitor", monitorId] });
    queryClient.invalidateQueries({ queryKey: ["metrics", monitorId] });
    queryClient.invalidateQueries({ queryKey: ["checks", monitorId] });
  }

  const monitor = monitorQuery.data ?? fallbackMonitor;
  const metrics = metricsQuery.data ?? demoMetrics;
  const checks = checksQuery.data ?? demoChecks;
  const timeline = (incidentsQuery.data ?? demoIncidents).filter((incident) => incident.monitorId === monitor.id);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Link to="/monitors" className="mb-2 inline-flex items-center gap-2 text-sm font-medium text-cobalt"><ArrowLeft className="h-4 w-4" />Monitors</Link>
          <div className="flex items-center gap-3">
            <h1 className="page-title">{monitor.name}</h1>
            <StatusBadge state={monitor.state} />
          </div>
          <p className="mt-1 text-sm text-slate-500">{monitor.method} {monitor.url}</p>
        </div>
        <div className="flex gap-2">
          <button className="button-secondary" onClick={() => runMutation.mutate()}><RefreshCw className="h-4 w-4" />Run check</button>
          {monitor.enabled ? (
            <button className="button-secondary" onClick={() => pauseMutation.mutate()}><Pause className="h-4 w-4" />Pause</button>
          ) : (
            <button className="button-secondary" onClick={() => resumeMutation.mutate()}><Play className="h-4 w-4" />Resume</button>
          )}
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <div className="panel p-4"><div className="text-sm text-slate-500">Uptime</div><div className="mt-2 text-2xl font-semibold">{metrics.uptimePercentage.toFixed(1)}%</div></div>
        <div className="panel p-4"><div className="text-sm text-slate-500">Average latency</div><div className="mt-2 text-2xl font-semibold">{Math.round(metrics.averageLatencyMs)} ms</div></div>
        <div className="panel p-4"><div className="text-sm text-slate-500">P95 latency</div><div className="mt-2 text-2xl font-semibold">{Math.round(metrics.p95LatencyMs)} ms</div></div>
        <div className="panel p-4"><div className="text-sm text-slate-500">Incidents</div><div className="mt-2 text-2xl font-semibold">{metrics.incidentCount}</div></div>
      </div>

      <section className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="panel p-4">
          <h2 className="section-title">Check latency</h2>
          <div className="mt-4 h-80">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={checks.map((check) => ({ time: new Date(check.checkedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }), latency: check.latencyMs ?? 0 }))}>
                <XAxis dataKey="time" stroke="#64748b" fontSize={12} />
                <YAxis stroke="#64748b" fontSize={12} />
                <Tooltip />
                <Line type="monotone" dataKey="latency" stroke="#0f766e" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="panel p-4">
          <h2 className="section-title">Incident timeline</h2>
          <div className="mt-4 space-y-4">
            {timeline.length === 0 ? <p className="text-sm text-slate-500">No incidents for this monitor.</p> : timeline.map((incident) => (
              <div key={incident.id} className="border-l-2 border-cobalt pl-3">
                <div className="text-sm font-semibold">{incident.title}</div>
                <div className="text-xs text-slate-500">{incident.status} · {new Date(incident.startedAt).toLocaleString()}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <div className="panel overflow-hidden">
        <div className="border-b border-line px-4 py-3 section-title">Recent checks</div>
        <div className="divide-y divide-line">
          {checks.slice(0, 10).map((check) => (
            <div key={check.id} className="grid grid-cols-4 gap-4 px-4 py-3 text-sm">
              <span>{new Date(check.checkedAt).toLocaleString()}</span>
              <span>{check.statusCode ?? "-"}</span>
              <span>{check.latencyMs ?? "-"} ms</span>
              <span className={check.success ? "text-emerald-700" : "text-red-700"}>{check.success ? "Success" : check.errorCategory}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
