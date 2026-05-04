import { FormEvent, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Bot, CheckCircle2, Send } from "lucide-react";
import { api, withFallback } from "../api/client";
import type { AiResponse, Monitor } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { useProjects } from "../ProjectContext";
import { demoMonitors } from "../mockData";

interface ChatMessage {
  role: "user" | "assistant";
  content: string;
  tools?: string[];
  citations?: string[];
  actionId?: string;
}

export function AiAssistantPage() {
  const { auth } = useAuth();
  const { selectedProject } = useProjects();
  const [message, setMessage] = useState("Summarize recent incidents and draft a customer-facing update.");
  const [monitorId, setMonitorId] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [pendingAction, setPendingAction] = useState<{ prompt: string; actionId: string }>();
  const monitorsQuery = useQuery({
    queryKey: ["monitors", selectedProject?.id],
    enabled: Boolean(selectedProject?.id),
    queryFn: () => withFallback(api.get<Monitor[]>(`/api/projects/${selectedProject?.id}/monitors`), demoMonitors)
  });
  const chatMutation = useMutation({
    mutationFn: (payload: { prompt: string; confirmedActionId?: string }) => api.post<AiResponse>("/api/ai/incident-assistant/chat", {
      organizationId: selectedProject?.organizationId ?? auth?.organizations[0]?.id,
      projectId: selectedProject?.id,
      monitorId: monitorId || undefined,
      message: payload.prompt,
      confirmedActionId: payload.confirmedActionId
    }),
    onSuccess: (response, variables) => {
      const data = response.data;
      setMessages((current) => [
        ...current,
        { role: "assistant", content: data.message, tools: data.toolsInvoked, citations: data.citations, actionId: data.actionId }
      ]);
      if (data.confirmationRequired && data.actionId) {
        setPendingAction({ prompt: variables.prompt, actionId: data.actionId });
      } else {
        setPendingAction(undefined);
      }
    }
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!message.trim()) {
      return;
    }
    const prompt = message.trim();
    setMessages((current) => [...current, { role: "user", content: prompt }]);
    chatMutation.mutate({ prompt });
    setMessage("");
  };

  const confirm = () => {
    if (!pendingAction) {
      return;
    }
    setMessages((current) => [...current, { role: "user", content: "Confirmed action: " + pendingAction.actionId }]);
    chatMutation.mutate({ prompt: pendingAction.prompt, confirmedActionId: pendingAction.actionId });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">AI Incident Assistant</h1>
        <p className="mt-1 text-sm text-slate-500">Uses monitor and incident data, and asks before write actions.</p>
      </div>
      <section className="grid gap-4 xl:grid-cols-[1fr_320px]">
        <div className="panel flex min-h-[620px] flex-col">
          <div className="flex items-center gap-2 border-b border-line px-4 py-3 section-title"><Bot className="h-4 w-4" />Assistant</div>
          <div className="flex-1 space-y-4 overflow-auto p-4">
            {messages.length === 0 ? <p className="text-sm text-slate-500">Ask for an incident summary, latency diagnosis, status update, or alert-rule suggestion.</p> : null}
            {messages.map((chat, index) => (
              <div key={index} className={`max-w-3xl rounded-lg px-4 py-3 ${chat.role === "user" ? "ml-auto bg-cobalt text-white" : "bg-slate-100 text-ink"}`}>
                <p className="whitespace-pre-wrap text-sm">{chat.content}</p>
                {chat.tools?.length ? <div className="mt-3 text-xs opacity-75">Tools: {chat.tools.join(", ")}</div> : null}
                {chat.citations?.length ? <div className="mt-1 text-xs opacity-75">Citations: {chat.citations.join(", ")}</div> : null}
              </div>
            ))}
          </div>
          {pendingAction ? (
            <div className="border-t border-line bg-amber-50 px-4 py-3">
              <button className="button-primary" onClick={confirm}><CheckCircle2 className="h-4 w-4" />Confirm alert-rule creation</button>
            </div>
          ) : null}
          <form className="flex gap-2 border-t border-line p-4" onSubmit={submit}>
            <input className="input" value={message} onChange={(event) => setMessage(event.target.value)} />
            <button className="button-primary" disabled={chatMutation.isPending}><Send className="h-4 w-4" />Send</button>
          </form>
        </div>
        <aside className="panel p-4">
          <h2 className="section-title">Context</h2>
          <label className="mt-4 block text-sm font-medium">Monitor</label>
          <select className="input mt-1" value={monitorId} onChange={(event) => setMonitorId(event.target.value)}>
            <option value="">Project-wide</option>
            {(monitorsQuery.data ?? demoMonitors).map((monitor) => <option key={monitor.id} value={monitor.id}>{monitor.name}</option>)}
          </select>
          <div className="mt-5 rounded-md border border-line bg-slate-50 p-3 text-sm text-slate-600">
            Write actions, including alert-rule creation, return a confirmation prompt before the frontend sends `confirmedActionId`.
          </div>
        </aside>
      </section>
    </div>
  );
}
