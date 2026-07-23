import { useState } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useDeleteEducation } from './hooks'
import { EducationDialog } from './EducationDialog'
import { formatDateRange } from './format'
import type { Education } from './types'

interface Props {
  educations: Education[]
}

export function EducationsSection({ educations }: Props) {
  const [dialogEducation, setDialogEducation] = useState<Education | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const deleteEducation = useDeleteEducation()

  const openAdd = () => {
    setDialogEducation(null)
    setDialogOpen(true)
  }
  const openEdit = (edu: Education) => {
    setDialogEducation(edu)
    setDialogOpen(true)
  }
  const handleDelete = (edu: Education) => {
    if (window.confirm(`Delete "${edu.institution}"?`)) {
      deleteEducation.mutate(edu.id)
    }
  }

  return (
    <section className="border-t border-slate-200 pt-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Education ({educations.length})</h2>
        <Button variant="outline" size="sm" onClick={openAdd}>
          <Plus className="h-4 w-4" />
          Add
        </Button>
      </div>

      {educations.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">No education yet. Add your degrees or courses.</p>
      ) : (
        <div className="mt-3 space-y-3">
          {educations.map((edu) => (
            <div
              key={edu.id}
              className="group rounded-md border border-slate-200 bg-white p-4 transition-shadow hover:shadow-sm"
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold text-slate-900">
                    {[edu.degree, edu.fieldOfStudy].filter(Boolean).join(', ') || edu.institution}
                  </p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {[
                      edu.degree || edu.fieldOfStudy ? edu.institution : null,
                      edu.location,
                      formatDateRange(edu.startDate, edu.endDate),
                    ]
                      .filter(Boolean)
                      .join(' · ')}
                  </p>
                </div>
                <div className="flex shrink-0 gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                  <button
                    type="button"
                    onClick={() => openEdit(edu)}
                    className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                    title="Edit"
                  >
                    <Pencil className="h-3.5 w-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(edu)}
                    className="rounded p-1 text-slate-400 hover:bg-red-50 hover:text-red-600"
                    title="Delete"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
              {edu.description && (
                <p className="mt-2 line-clamp-3 text-sm text-slate-600">{edu.description}</p>
              )}
            </div>
          ))}
        </div>
      )}

      <EducationDialog education={dialogEducation} open={dialogOpen} onOpenChange={setDialogOpen} />
    </section>
  )
}
