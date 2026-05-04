import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BellPlus } from "lucide-react";
import { api, withFallback } from "../api/client";
import type { AlertCondition, AlertRule, Monitor, NotificationChannel } from "../api/types";
import { useProjects } from "../ProjectContext";
import { demoAlertRules, demoChannels, demoMonitors } from "../mockData";

export function AlertRulesPage() {
  const { selectedProject } = useProjects();
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    name: "High latency",
    condition: "LATENCY_THRESHOLD_EXCEEDED" as AlertCondition,
    thresholdMs: 750,
    monitorId: "",
    notificationChannelId: "",
    enabled: true
  });
  const projectId = selectedProject?.id;
  const rulesQuery = useQuery({ queryKey: ["alert-rules", projectId], enabled: Boolean(projectId), queryFn: () => withFallback(api.get<AlertRule[]>(`/api/projects/${projectId}/alert-rules`), demoAlertRules) });
  const monitorsQuery = useQuery({ queryKey: ["monitors", projectId], enabled: Boolean(projectId), queryFn: () => withFallback(api.get<Monitor[]>(`/api/projects/${projectId}/monitors`), demoMonitors) });
  const channelsQuery = useQuery({ queryKey: ["channels", projectId], enabled: Boolean(projectId), queryFn: () => withFallback(api.get<NotificationChannel[]>(`/api/projects/${projectId}/notification-channels`), demoChannels) });
  const createMutation = useMutation({
    mutationFn: () => api.post(`/api/projects/${projectId}/alert-rules`, {
      ...form,
      monitorId: form.monitorId || null,
      notificationChannelId: form.notificationChannelId || null
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["alert-rules", projectId] })
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    createMutation.mutate();
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">Alert Rules</h1>
        <p className="mt-1 text-sm text-slate-500">Webhook-backed conditions for monitor down, recovery, and latency thresholds.</p>
      </div>
      <section className="grid gap-4 xl:grid-cols-[1fr_420px]">
        <div className="panel overflow-hidden">
          <div className="border-b border-line px-4 py-3 section-title">Rules</div>
          <div className="divide-y divide-line">
            {(rulesQuery.data ?? demoAlertRules).map((rule) => (
              <div key={rule.id} className="grid gap-2 px-4 py-4 md:grid-cols-[1fr_190px_120px]">
                <div><div className="font-semibold">{rule.name}</div><div className="text-sm text-slate-500">{rule.condition}</div></div>
                <div className="text-sm text-slate-500">{rule.thresholdMs ? `${rule.thresholdMs} ms` : "No threshold"}</div>
                <div className={rule.enabled ? "text-sm font-semibold text-emerald-700" : "text-sm font-semibold text-slate-500"}>{rule.enabled ? "Enabled" : "Disabled"}</div>
              </div>
            ))}
          </div>
        </div>
        <form className="panel p-4" onSubmit={submit}>
          <h2 className="section-title">Create rule</h2>
          <div className="mt-4 space-y-3">
            <input className="input" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            <select className="input" value={form.condition} onChange={(event) => setForm({ ...form, condition: event.target.value as AlertCondition })}>
              <option value="MONITOR_DOWN">Monitor down</option>
              <option value="MONITOR_RECOVERED">Monitor recovered</option>
              <option value="LATENCY_THRESHOLD_EXCEEDED">Latency threshold</option>
            </select>
            <select className="input" value={form.monitorId} onChange={(event) => setForm({ ...form, monitorId: event.target.value })}>
              <option value="">All monitors</option>
              {(monitorsQuery.data ?? demoMonitors).map((monitor) => <option key={monitor.id} value={monitor.id}>{monitor.name}</option>)}
            </select>
            <select className="input" value={form.notificationChannelId} onChange={(event) => setForm({ ...form, notificationChannelId: event.target.value })}>
              <option value="">No channel</option>
              {(channelsQuery.data ?? demoChannels).map((channel) => <option key={channel.id} value={channel.id}>{channel.name}</option>)}
            </select>
            <input className="input" type="number" value={form.thresholdMs} onChange={(event) => setForm({ ...form, thresholdMs: Number(event.target.value) })} />
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.enabled} onChange={(event) => setForm({ ...form, enabled: event.target.checked })} /> Enabled</label>
            <button className="button-primary w-full"><BellPlus className="h-4 w-4" />Create rule</button>
          </div>
        </form>
      </section>
    </div>
  );
}
