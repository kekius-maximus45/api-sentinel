import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { KeyRound, Plus } from "lucide-react";
import { api, withFallback } from "../api/client";
import type { ApiKey } from "../api/types";
import { useProjects } from "../ProjectContext";
import { demoApiKeys } from "../mockData";

export function ApiKeysPage() {
  const { selectedProject } = useProjects();
  const queryClient = useQueryClient();
  const [name, setName] = useState("Local MCP client");
  const [secret, setSecret] = useState<string>();
  const organizationId = selectedProject?.organizationId;
  const keysQuery = useQuery({
    queryKey: ["api-keys", organizationId],
    enabled: Boolean(organizationId),
    queryFn: () => withFallback(api.get<ApiKey[]>(`/api/api-keys?organizationId=${organizationId}`), demoApiKeys)
  });
  const createMutation = useMutation({
    mutationFn: () => api.post<{ apiKey: ApiKey; secret: string }>("/api/api-keys", { organizationId, name }),
    onSuccess: (response) => {
      setSecret(response.data.secret);
      queryClient.invalidateQueries({ queryKey: ["api-keys", organizationId] });
    }
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    createMutation.mutate();
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">API Keys</h1>
        <p className="mt-1 text-sm text-slate-500">Scoped keys for MCP clients and automation. Secrets are shown once.</p>
      </div>
      {secret ? (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4">
          <div className="text-sm font-semibold text-emerald-800">New API key secret</div>
          <code className="mt-2 block break-all rounded bg-white p-3 text-sm text-emerald-900">{secret}</code>
        </div>
      ) : null}
      <section className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="panel overflow-hidden">
          <div className="border-b border-line px-4 py-3 section-title">Keys</div>
          <div className="divide-y divide-line">
            {(keysQuery.data ?? demoApiKeys).map((key) => (
              <div key={key.id} className="flex items-center justify-between gap-4 px-4 py-4">
                <div>
                  <div className="font-semibold">{key.name}</div>
                  <div className="text-sm text-slate-500">Prefix {key.keyPrefix}</div>
                </div>
                <KeyRound className="h-4 w-4 text-cobalt" />
              </div>
            ))}
          </div>
        </div>
        <form className="panel p-4" onSubmit={submit}>
          <h2 className="section-title">Create key</h2>
          <input className="input mt-4" value={name} onChange={(event) => setName(event.target.value)} />
          <button className="button-primary mt-3 w-full"><Plus className="h-4 w-4" />Create API key</button>
        </form>
      </section>
    </div>
  );
}
