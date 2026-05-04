import type { MonitorState } from "../api/types";

const styles: Record<MonitorState, string> = {
  UP: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  DEGRADED: "bg-amber-50 text-amber-700 ring-amber-200",
  DOWN: "bg-red-50 text-red-700 ring-red-200",
  PAUSED: "bg-slate-100 text-slate-600 ring-slate-200"
};

export function StatusBadge({ state }: { state: MonitorState }) {
  return <span className={`inline-flex items-center rounded px-2 py-1 text-xs font-semibold ring-1 ${styles[state]}`}>{state}</span>;
}
