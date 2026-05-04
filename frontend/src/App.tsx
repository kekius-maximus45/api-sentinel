import { Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { useAuth } from "./auth/AuthContext";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { MonitorsPage } from "./pages/MonitorsPage";
import { MonitorDetailPage } from "./pages/MonitorDetailPage";
import { IncidentsPage } from "./pages/IncidentsPage";
import { AlertRulesPage } from "./pages/AlertRulesPage";
import { NotificationChannelsPage } from "./pages/NotificationChannelsPage";
import { StatusPreviewPage } from "./pages/StatusPreviewPage";
import { AiAssistantPage } from "./pages/AiAssistantPage";
import { ApiKeysPage } from "./pages/ApiKeysPage";

export default function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route element={isAuthenticated ? <Layout /> : <Navigate to="/login" replace />}>
        <Route index element={<DashboardPage />} />
        <Route path="monitors" element={<MonitorsPage />} />
        <Route path="monitors/:monitorId" element={<MonitorDetailPage />} />
        <Route path="incidents" element={<IncidentsPage />} />
        <Route path="alerts" element={<AlertRulesPage />} />
        <Route path="channels" element={<NotificationChannelsPage />} />
        <Route path="status-preview" element={<StatusPreviewPage />} />
        <Route path="assistant" element={<AiAssistantPage />} />
        <Route path="api-keys" element={<ApiKeysPage />} />
      </Route>
    </Routes>
  );
}
