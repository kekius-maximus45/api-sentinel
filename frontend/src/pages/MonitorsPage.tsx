import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Plus, PlayCircle } from "lucide-react";
import { api, withFallback } from "../api/client";
import type { Monitor, MonitorMethod, MonitorRequest } from "../api/types";
import { StatusBadge } from "../components/StatusBadge";
import { useProjects } from "../ProjectContext";
import { demoMonitors } from "../mockData";

const initialForm: MonitorRequest = {
  name: "",
  url: "https://example.com/health",
  method: "GET",
  expectedStatusCode: 200,
  timeoutSeconds: 5,
  intervalSeconds: 300,
  latencyThresholdMs: 750,
  failureThreshold: 3,
  headers: {},
  enabled: true
};

export function MonitorsPage() {
  const { selectedProject } = useProjects();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<MonitorRequest>(initialForm);
  const projectId = selectedProject?.id;
  const monitorsQuery = useQuery({
    queryKey: ["monitors", projectId],
    enabled: Boolean(projectId),
    queryFn: () => withFallback(api.get<Monitor[]>(`/api/projects/${projectId}/monitors`), demoMonitors)
  });
  const createMutation = useMutation({
    mutationFn: (payload: MonitorRequest) => api.post<Monitor>(`/api/projects/${projectId}/monitors`, payload),
    onSuccess: () => {
      setForm(initialForm);
      queryClient.invalidateQueries({ queryKey: ["monitors", projectId] });
    }
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    createMutation.mutate(form);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">Monitors</h1>
        <p className="mt-1 text-sm text-slate-500">HTTP checks with interval, timeout, threshold, and failure policy.</p>
      </div>

      <section className="grid gap-4 xl:grid-cols-[1fr_420px]">
        <div className="panel overflow-hidden">
          <div className="border-b border-line px-4 py-3 section-title">Configured monitors</div>
          <div className="divide-y divide-line">
            {(monitorsQuery.data ?? demoMonitors).map((monitor) => (
              <Link key={monitor.id} to={`/monitors/${monitor.id}`} className="flex items-center justify-between gap-4 px-4 py-4 hover:bg-slate-50">
                <div className="min-w-0">
                  <div className="font-semibold">{monitor.name}</div>
                  <div className="truncate text-sm text-slate-500">{monitor.method} {monitor.url}</div>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  <span className="text-sm text-slate-500">{monitor.intervalSeconds}s</span>
                  <StatusBadge state={monitor.state} />
                </div>
              </Link>
            ))}
          </div>
        </div>

        <form className="panel p-4" onSubmit={submit}>
          <h2 className="section-title">Create monitor</h2>
          <div className="mt-4 space-y-3">
            <input className="input" placeholder="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
            <input className="input" placeholder="https://api.example.com/health" value={form.url} onChange={(event) => setForm({ ...form, url: event.target.value })} required />
            <div className="grid grid-cols-2 gap-3">
              <select className="input" value={form.method} onChange={(event) => setForm({ ...form, method: event.target.value as MonitorMethod })}>
                {["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD"].map((method) => <option key={method}>{method}</option>)}
              </select>
              <input className="input" type="number" value={form.expectedStatusCode} onChange={(event) => setForm({ ...form, expectedStatusCode: Number(event.target.value) })} />
              <input className="input" type="number" value={form.intervalSeconds} min={60} onChange={(event) => setForm({ ...form, intervalSeconds: Number(event.target.value) })} />
              <input className="input" type="number" value={form.timeoutSeconds} min={1} max={30} onChange={(event) => setForm({ ...form, timeoutSeconds: Number(event.target.value) })} />
              <input className="input" type="number" value={form.latencyThresholdMs} onChange={(event) => setForm({ ...form, latencyThresholdMs: Number(event.target.value) })} />
              <input className="input" type="number" value={form.failureThreshold} onChange={(event) => setForm({ ...form, failureThreshold: Number(event.target.value) })} />
            </div>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.enabled} onChange={(event) => setForm({ ...form, enabled: event.target.checked })} /> Enabled</label>
            <button className="button-primary w-full" disabled={createMutation.isPending}><Plus className="h-4 w-4" />Create monitor</button>
            <button type="button" className="button-secondary w-full"><PlayCircle className="h-4 w-4" />Manual checks are available on detail pages</button>
          </div>
        </form>
      </section>
    </div>
  );
}
