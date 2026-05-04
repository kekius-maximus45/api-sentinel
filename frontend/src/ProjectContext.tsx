import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api, withFallback } from "./api/client";
import type { Project } from "./api/types";
import { demoProjects } from "./mockData";
import { useAuth } from "./auth/AuthContext";

interface ProjectContextValue {
  projects: Project[];
  selectedProject?: Project;
  selectedProjectId?: string;
  setSelectedProjectId: (projectId: string) => void;
}

const ProjectContext = createContext<ProjectContextValue | undefined>(undefined);

export function ProjectProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  const [selectedProjectId, setSelectedProjectId] = useState<string | undefined>();
  const projectsQuery = useQuery({
    queryKey: ["projects"],
    enabled: isAuthenticated,
    queryFn: () => withFallback(api.get<Project[]>("/api/projects"), demoProjects)
  });
  const projects = projectsQuery.data ?? demoProjects;

  useEffect(() => {
    if (!selectedProjectId && projects.length > 0) {
      setSelectedProjectId(projects[0].id);
    }
  }, [projects, selectedProjectId]);

  const value = useMemo<ProjectContextValue>(() => ({
    projects,
    selectedProjectId,
    selectedProject: projects.find((project) => project.id === selectedProjectId) ?? projects[0],
    setSelectedProjectId
  }), [projects, selectedProjectId]);

  return <ProjectContext.Provider value={value}>{children}</ProjectContext.Provider>;
}

export function useProjects() {
  const context = useContext(ProjectContext);
  if (!context) {
    throw new Error("useProjects must be used inside ProjectProvider");
  }
  return context;
}
