import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, MessageSquare } from "lucide-react";
import { api, withFallback } from "../api/client";
import type { Incident } from "../api/types";
import { useProjects } from "../ProjectContext";
import { demoIncidents } from "../mockData";

export function IncidentsPage() {
  const { selectedProject } = useProjects();
  const queryClient = useQueryClient();
  const [note, setNote] = useState<Record<string, string>>({});
  const incidentsQuery = useQuery({
    queryKey: ["incidents", selectedProject?.id],
    enabled: Boolean(selectedProject?.id),
    queryFn: () => withFallback(api.get<Incident[]>(`/api/projects/${selectedProject?.id}/incidents`), demoIncidents)
  });
  const resolveMutation = useMutation({
    mutationFn: (incidentId: string) => api.post(`/api/incidents/${incidentId}/resolve`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["incidents", selectedProject?.id] })
  });
  const noteMutation = useMutation({
    mutationFn: ({ incidentId, message }: { incidentId: string; message: string }) => api.post(`/api/incidents/${incidentId}/notes`, { message }),
    onSuccess: () => {
      setNote({});
      queryClient.invalidateQueries({ queryKey: ["incidents", selectedProject?.id] });
    }
  });

  const submitNote = (event: FormEvent, incidentId: string) => {
    event.preventDefault();
    const message = note[incidentId];
    if (message?.trim()) {
      noteMutation.mutate({ incidentId, message });
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">Incidents</h1>
        <p className="mt-1 text-sm text-slate-500">Automatic and manual incident lifecycle for {selectedProject?.name}.</p>
      </div>
      <div className="space-y-4">
        {(incidentsQuery.data ?? demoIncidents).map((incident) => (
          <section key={incident.id} className="panel p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="font-semibold">{incident.title}</h2>
                <p className="mt-1 text-sm text-slate-500">{incident.monitorName} · {new Date(incident.startedAt).toLocaleString()}</p>
              </div>
              <div className={`rounded px-2 py-1 text-xs font-semibold ${incident.status === "OPEN" ? "bg-red-50 text-red-700" : "bg-emerald-50 text-emerald-700"}`}>{incident.status}</div>
            </div>
            <div className="mt-4 space-y-3">
              {incident.events.map((event) => (
                <div key={event.id} className="flex gap-3 text-sm">
                  <span className="mt-1 h-2 w-2 rounded-full bg-cobalt" />
                  <div><span className="font-medium">{event.type}</span> {event.message}<div className="text-xs text-slate-500">{new Date(event.createdAt).toLocaleString()}</div></div>
                </div>
              ))}
            </div>
            <form className="mt-4 flex gap-2" onSubmit={(event) => submitNote(event, incident.id)}>
              <input className="input" placeholder="Add an operator note" value={note[incident.id] ?? ""} onChange={(event) => setNote({ ...note, [incident.id]: event.target.value })} />
              <button className="button-secondary"><MessageSquare className="h-4 w-4" />Note</button>
              {incident.status === "OPEN" ? <button type="button" className="button-primary" onClick={() => resolveMutation.mutate(incident.id)}><CheckCircle2 className="h-4 w-4" />Resolve</button> : null}
            </form>
          </section>
        ))}
      </div>
    </div>
  );
}
