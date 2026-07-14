import { useState } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useDeleteExperience } from './hooks'
import { ExperienceDialog } from './ExperienceDialog'
import { formatDateRange } from './format'
import type { Experience } from './types'

interface Props {
  experiences: Experience[]
}

export function ExperiencesSection({ experiences }: Props) {
  const [dialogExperience, setDialogExperience] = useState<Experience | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const deleteExperience = useDeleteExperience()

  const openAdd = () => {
    setDialogExperience(null)
    setDialogOpen(true)
  }
  const openEdit = (exp: Experience) => {
    setDialogExperience(exp)
    setDialogOpen(true)
  }
  const handleDelete = (exp: Experience) => {
    if (window.confirm(`Delete "${exp.position}" at ${exp.company}?`)) {
      deleteExperience.mutate(exp.id)
    }
  }

  return (
    <section className="border-t border-slate-200 pt-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Experience ({experiences.length})</h2>
        <Button variant="outline" size="sm" onClick={openAdd}>
          <Plus className="h-4 w-4" />
          Add
        </Button>
      </div>

      {experiences.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">No experience yet. Add your work experience.</p>
      ) : (
        <div className="mt-3 space-y-3">
          {experiences.map((exp) => (
            <div
              key={exp.id}
              className="group rounded-md border border-slate-200 bg-white p-4 transition-shadow hover:shadow-sm"
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold text-slate-900">
                    {exp.position} <span className="font-normal text-slate-500">@ {exp.company}</span>
                  </p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {[exp.location, formatDateRange(exp.startDate, exp.endDate)].filter(Boolean).join(' · ')}
                  </p>
                </div>
                <div className="flex shrink-0 gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                  <button
                    type="button"
                    onClick={() => openEdit(exp)}
                    className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                    title="Edit"
                  >
                    <Pencil className="h-3.5 w-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(exp)}
                    className="rounded p-1 text-slate-400 hover:bg-red-50 hover:text-red-600"
                    title="Delete"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
              {exp.description && (
                <p className="mt-2 line-clamp-3 text-sm text-slate-600">{exp.description}</p>
              )}
            </div>
          ))}
        </div>
      )}

      <ExperienceDialog experience={dialogExperience} open={dialogOpen} onOpenChange={setDialogOpen} />
    </section>
  )
}
