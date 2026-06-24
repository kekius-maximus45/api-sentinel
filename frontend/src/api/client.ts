import axios from "axios";

// In production (Vercel), the frontend is proxied to the backend via vercel.json rewrites,
// so the base URL is an empty string (same origin). In local dev, point at the backend port.
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/+$/, "");

export const api = axios.create({
  baseURL: API_BASE_URL
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("api-sentinel-token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auto-clear stale credentials on 401 so the user is sent back to login
// rather than seeing a broken dashboard.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      localStorage.removeItem("api-sentinel-token");
      localStorage.removeItem("api-sentinel-auth");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export async function withFallback<T>(request: Promise<{ data: T }>, fallback: T): Promise<T> {
  try {
    const response = await request;
    return response.data;
  } catch {
    return fallback;
  }
}
