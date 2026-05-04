import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Send, Webhook } from "lucide-react";
import { api, withFallback } from "../api/client";
import type { NotificationChannel } from "../api/types";
import { useProjects } from "../ProjectContext";
import { demoChannels } from "../mockData";

export function NotificationChannelsPage() {
  const { selectedProject } = useProjects();
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ name: "Ops webhook", webhookUrl: "https://hooks.example.com/api-sentinel" });
  const projectId = selectedProject?.id;
  const channelsQuery = useQuery({
    queryKey: ["channels", projectId],
    enabled: Boolean(projectId),
    queryFn: () => withFallback(api.get<NotificationChannel[]>(`/api/projects/${projectId}/notification-channels`), demoChannels)
  });
  const createMutation = useMutation({
    mutationFn: () => api.post(`/api/projects/${projectId}/notification-channels`, form),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["channels", projectId] })
  });
  const testMutation = useMutation({
    mutationFn: (channelId: string) => api.post(`/api/notification-channels/${channelId}/test`)
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    createMutation.mutate();
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">Notification Channels</h1>
        <p className="mt-1 text-sm text-slate-500">Webhook destinations used by alert rules.</p>
      </div>
      <section className="grid gap-4 xl:grid-cols-[1fr_420px]">
        <div className="panel overflow-hidden">
          <div className="border-b border-line px-4 py-3 section-title">Channels</div>
          <div className="divide-y divide-line">
            {(channelsQuery.data ?? demoChannels).map((channel) => (
              <div key={channel.id} className="flex items-center justify-between gap-4 px-4 py-4">
                <div className="min-w-0">
                  <div className="font-semibold">{channel.name}</div>
                  <div className="truncate text-sm text-slate-500">{channel.webhookUrl}</div>
                </div>
                <button className="button-secondary" onClick={() => testMutation.mutate(channel.id)}><Send className="h-4 w-4" />Test</button>
              </div>
            ))}
          </div>
        </div>
        <form className="panel p-4" onSubmit={submit}>
          <h2 className="section-title">Add webhook</h2>
          <div className="mt-4 space-y-3">
            <input className="input" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            <input className="input" value={form.webhookUrl} onChange={(event) => setForm({ ...form, webhookUrl: event.target.value })} />
            <button className="button-primary w-full"><Webhook className="h-4 w-4" />Create channel</button>
          </div>
        </form>
      </section>
    </div>
  );
}
