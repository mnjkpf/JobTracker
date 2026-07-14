import { useState } from 'react'
import { ExternalLink, Pencil, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useDeleteProject } from './hooks'
import { ProjectDialog } from './ProjectDialog'
import { formatDateRange } from './format'
import type { Project } from './types'

interface Props {
  projects: Project[]
}

export function ProjectsSection({ projects }: Props) {
  const [dialogProject, setDialogProject] = useState<Project | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const deleteProject = useDeleteProject()

  const openAdd = () => {
    setDialogProject(null)
    setDialogOpen(true)
  }
  const openEdit = (project: Project) => {
    setDialogProject(project)
    setDialogOpen(true)
  }
  const handleDelete = (project: Project) => {
    if (window.confirm(`Delete project "${project.name}"?`)) {
      deleteProject.mutate(project.id)
    }
  }

  return (
    <section className="border-t border-slate-200 pt-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Projects ({projects.length})</h2>
        <Button variant="outline" size="sm" onClick={openAdd}>
          <Plus className="h-4 w-4" />
          Add
        </Button>
      </div>

      {projects.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">No projects yet. Add a project you've built.</p>
      ) : (
        <div className="mt-3 space-y-3">
          {projects.map((project) => (
            <div
              key={project.id}
              className="group rounded-md border border-slate-200 bg-white p-4 transition-shadow hover:shadow-sm"
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <div className="flex items-center gap-1.5">
                    <p className="font-semibold text-slate-900">{project.name}</p>
                    {project.url && (
                      <a
                        href={project.url}
                        target="_blank"
                        rel="noreferrer"
                        className="text-slate-400 hover:text-slate-700"
                      >
                        <ExternalLink className="h-3.5 w-3.5" />
                      </a>
                    )}
                  </div>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {[project.technologies, formatDateRange(project.startDate, project.endDate)]
                      .filter(Boolean)
                      .join(' · ')}
                  </p>
                </div>
                <div className="flex shrink-0 gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                  <button
                    type="button"
                    onClick={() => openEdit(project)}
                    className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                    title="Edit"
                  >
                    <Pencil className="h-3.5 w-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(project)}
                    className="rounded p-1 text-slate-400 hover:bg-red-50 hover:text-red-600"
                    title="Delete"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
              {project.description && (
                <p className="mt-2 line-clamp-3 text-sm text-slate-600">{project.description}</p>
              )}
            </div>
          ))}
        </div>
      )}

      <ProjectDialog project={dialogProject} open={dialogOpen} onOpenChange={setDialogOpen} />
    </section>
  )
}
