import { FormEvent, useState } from "react";
import axios from "axios";
import { Activity, LogIn, UserPlus } from "lucide-react";
import { useAuth } from "../auth/AuthContext";

const IS_DEV = import.meta.env.DEV;

export function LoginPage() {
  const { login, register, startDemo } = useAuth();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [organizationName, setOrganizationName] = useState("");
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(undefined);
    try {
      if (mode === "login") {
        await login(email, password);
      } else {
        await register(displayName, organizationName, email, password);
      }
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const message = (err.response?.data as { message?: string } | undefined)?.message;
        setError(message ?? "Authentication failed. Please check your credentials and try again.");
      } else {
        setError("Authentication failed. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="grid min-h-screen place-items-center bg-panel px-4">
      <section className="w-full max-w-md rounded-lg border border-line bg-white p-6 shadow-panel">
        <div className="flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-lg bg-cobalt text-white"><Activity className="h-5 w-5" /></span>
          <div>
            <h1 className="text-lg font-semibold text-ink">API Sentinel</h1>
            <p className="text-sm text-slate-500">Operations console</p>
          </div>
        </div>

        <div className="mt-6 grid grid-cols-2 rounded-md bg-slate-100 p-1">
          <button className={`rounded px-3 py-2 text-sm font-semibold ${mode === "login" ? "bg-white shadow-sm" : "text-slate-600"}`} onClick={() => setMode("login")}>Login</button>
          <button className={`rounded px-3 py-2 text-sm font-semibold ${mode === "register" ? "bg-white shadow-sm" : "text-slate-600"}`} onClick={() => setMode("register")}>Register</button>
        </div>

        <form className="mt-5 space-y-4" onSubmit={submit}>
          {mode === "register" ? (
            <>
              <label className="block text-sm font-medium">Name<input className="input mt-1" value={displayName} onChange={(event) => setDisplayName(event.target.value)} /></label>
              <label className="block text-sm font-medium">Organization<input className="input mt-1" value={organizationName} onChange={(event) => setOrganizationName(event.target.value)} /></label>
            </>
          ) : null}
          <label className="block text-sm font-medium">Email<input className="input mt-1" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label>
          <label className="block text-sm font-medium">Password<input className="input mt-1" type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          {error ? <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p> : null}
          <button className="button-primary w-full" disabled={loading}>
            {mode === "login" ? <LogIn className="h-4 w-4" /> : <UserPlus className="h-4 w-4" />}
            {loading ? "Working…" : mode === "login" ? "Login" : "Create account"}
          </button>
          {IS_DEV && (
            <button type="button" className="button-secondary w-full" onClick={startDemo}>
              Open demo workspace (dev only)
            </button>
          )}
        </form>
      </section>
    </main>
  );
}
