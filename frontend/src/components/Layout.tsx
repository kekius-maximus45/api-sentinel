import { Activity, AlertTriangle, Bell, Bot, Gauge, Globe2, KeyRound, LayoutDashboard, LogOut, ServerCog } from "lucide-react";
import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ProjectProvider, useProjects } from "../ProjectContext";

const navItems = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/monitors", label: "Monitors", icon: Activity },
  { to: "/incidents", label: "Incidents", icon: AlertTriangle },
  { to: "/alerts", label: "Alert Rules", icon: Bell },
  { to: "/channels", label: "Channels", icon: ServerCog },
  { to: "/status-preview", label: "Status Page", icon: Globe2 },
  { to: "/assistant", label: "AI Assistant", icon: Bot },
  { to: "/api-keys", label: "API Keys", icon: KeyRound }
];

export function Layout() {
  return (
    <ProjectProvider>
      <AppFrame />
    </ProjectProvider>
  );
}

function AppFrame() {
  const { logout, auth } = useAuth();
  const { projects, selectedProjectId, setSelectedProjectId } = useProjects();

  return (
    <div className="min-h-screen bg-panel text-ink">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-line bg-white px-4 py-5 lg:block">
        <Link to="/" className="flex items-center gap-3 px-2 text-lg font-semibold">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-cobalt text-white"><Gauge className="h-5 w-5" /></span>
          API Sentinel
        </Link>
        <nav className="mt-8 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${isActive ? "bg-blue-50 text-cobalt" : "text-graphite hover:bg-slate-100"}`}
            >
              <item.icon className="h-4 w-4" aria-hidden="true" />
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 border-b border-line bg-white/95 backdrop-blur">
          <div className="flex h-16 items-center justify-between gap-4 px-4 sm:px-6">
            <div className="min-w-0">
              <div className="text-sm font-semibold">{auth?.user.displayName ?? "Operator"}</div>
              <div className="truncate text-xs text-slate-500">{auth?.user.email}</div>
            </div>
            <div className="flex items-center gap-3">
              <select
                className="h-10 rounded-md border border-line bg-white px-3 text-sm"
                value={selectedProjectId ?? ""}
                onChange={(event) => setSelectedProjectId(event.target.value)}
                aria-label="Project selector"
              >
                {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
              </select>
              <button className="inline-flex h-10 w-10 items-center justify-center rounded-md border border-line text-graphite hover:bg-slate-100" onClick={logout} title="Log out">
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          </div>
        </header>
        <main className="px-4 py-6 sm:px-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
