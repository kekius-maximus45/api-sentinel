import type { LucideIcon } from "lucide-react";

export function MetricCard({ icon: Icon, label, value, detail }: { icon: LucideIcon; label: string; value: string; detail?: string }) {
  return (
    <div className="rounded-lg border border-line bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <span className="text-sm font-medium text-graphite">{label}</span>
        <Icon className="h-4 w-4 text-cobalt" aria-hidden="true" />
      </div>
      <div className="mt-3 text-2xl font-semibold text-ink">{value}</div>
      {detail ? <div className="mt-1 text-sm text-slate-500">{detail}</div> : null}
    </div>
  );
}
