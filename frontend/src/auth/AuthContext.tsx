import { createContext, useContext, useMemo, useState } from "react";
import { api } from "../api/client";
import type { AuthResponse } from "../api/types";

interface AuthContextValue {
  auth?: AuthResponse;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (displayName: string, organizationName: string, email: string, password: string) => Promise<void>;
  startDemo: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthResponse | undefined>(() => {
    const raw = localStorage.getItem("api-sentinel-auth");
    return raw ? (JSON.parse(raw) as AuthResponse) : undefined;
  });

  const persist = (response: AuthResponse) => {
    localStorage.setItem("api-sentinel-token", response.accessToken);
    localStorage.setItem("api-sentinel-auth", JSON.stringify(response));
    setAuth(response);
  };

  const value = useMemo<AuthContextValue>(() => ({
    auth,
    isAuthenticated: Boolean(auth?.accessToken),
    async login(email, password) {
      const response = await api.post<AuthResponse>("/api/auth/login", { email, password });
      persist(response.data);
    },
    async register(displayName, organizationName, email, password) {
      const response = await api.post<AuthResponse>("/api/auth/register", { displayName, organizationName, email, password });
      persist(response.data);
    },
    startDemo() {
      persist({
        accessToken: "demo-token",
        user: {
          id: "99999999-9999-9999-9999-999999999999",
          email: "operator@example.com",
          displayName: "Demo Operator"
        },
        organizations: [
          {
            id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            name: "Demo Org",
            role: "OWNER"
          }
        ]
      });
    },
    logout() {
      localStorage.removeItem("api-sentinel-token");
      localStorage.removeItem("api-sentinel-auth");
      setAuth(undefined);
    }
  }), [auth]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
