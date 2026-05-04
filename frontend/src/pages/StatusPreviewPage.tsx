import { useQuery } from "@tanstack/react-query";
import { ExternalLink } from "lucide-react";
import { api, withFallback } from "../api/client";
import type { StatusPage } from "../api/types";
import { StatusBadge } from "../components/StatusBadge";
import { useProjects } from "../ProjectContext";
import { demoStatusPage } from "../mockData";

export function StatusPreviewPage() {
  const { selectedProject } = useProjects();
  const statusQuery = useQuery({
    queryKey: ["status-page", selectedProject?.slug],
    enabled: Boolean(selectedProject?.slug),
    queryFn: () => withFallback(api.get<StatusPage>(`/api/public/status/${selectedProject?.slug}`), demoStatusPage)
  });
  const status = statusQuery.data ?? demoStatusPage;
  const publicUrl = `${api.defaults.baseURL}/status/${status.slug}`;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="page-title">Public Status Page</h1>
          <p className="mt-1 text-sm text-slate-500">Privacy-safe customer view for {status.projectName}.</p>
        </div>
        <a className="button-secondary" href={publicUrl} target="_blank" rel="noreferrer"><ExternalLink className="h-4 w-4" />Open public page</a>
      </div>

      <section className="rounded-lg border border-line bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line pb-5">
          <div>
            <h2 className="text-2xl font-semibold">{status.projectName}</h2>
            <p className="mt-1 text-sm text-slate-500">Service status</p>
          </div>
          <StatusBadge state={status.overallStatus} />
        </div>
        <div className="mt-6 divide-y divide-line">
          {status.monitors.map((monitor) => (
            <div key={monitor.id} className="flex items-center justify-between py-4">
              <div>
                <div className="font-semibold">{monitor.name}</div>
                <div className="text-sm text-slate-500">{monitor.lastLatencyMs ? `${monitor.lastLatencyMs} ms` : "No check yet"}</div>
              </div>
              <StatusBadge state={monitor.state} />
            </div>
          ))}
        </div>
        <div className="mt-6">
          <h3 className="section-title">Active incidents</h3>
          <div className="mt-3 space-y-3">
            {status.activeIncidents.length === 0 ? <p className="text-sm text-slate-500">No active incidents.</p> : status.activeIncidents.map((incident) => (
              <div key={incident.id} className="rounded-md border border-line p-3">
                <div className="font-semibold">{incident.title}</div>
                <div className="text-sm text-slate-500">{incident.monitorName} · {new Date(incident.startedAt).toLocaleString()}</div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
