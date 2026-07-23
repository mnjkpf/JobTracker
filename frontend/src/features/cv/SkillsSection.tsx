import { useState } from 'react'
import { Pencil, Plus, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useDeleteSkill } from './hooks'
import { SkillDialog } from './SkillDialog'
import type { Skill } from './types'

interface Props {
  skills: Skill[]
}

export function SkillsSection({ skills }: Props) {
  const [dialogSkill, setDialogSkill] = useState<Skill | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const deleteSkill = useDeleteSkill()

  const openAdd = () => {
    setDialogSkill(null)
    setDialogOpen(true)
  }
  const openEdit = (skill: Skill) => {
    setDialogSkill(skill)
    setDialogOpen(true)
  }

  return (
    <section className="border-t border-slate-200 pt-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Skills ({skills.length})</h2>
        <Button variant="outline" size="sm" onClick={openAdd}>
          <Plus className="h-4 w-4" />
          Add
        </Button>
      </div>

      {skills.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">No skills yet. Add your technical skills.</p>
      ) : (
        <div className="mt-3 flex flex-wrap gap-2">
          {skills.map((skill) => (
            <div
              key={skill.id}
              className="group flex items-center gap-1.5 rounded-full border border-slate-200 bg-white py-1 pl-3 pr-1.5 text-sm text-slate-700"
            >
              <span>
                {skill.name}
                {skill.level && <span className="text-slate-400"> · {skill.level.toLowerCase()}</span>}
              </span>
              <button
                type="button"
                onClick={() => openEdit(skill)}
                className="rounded p-0.5 text-slate-300 opacity-0 hover:bg-slate-100 hover:text-slate-600 group-hover:opacity-100"
                title="Edit level"
              >
                <Pencil className="h-3 w-3" />
              </button>
              <button
                type="button"
                onClick={() => deleteSkill.mutate(skill.id)}
                className="rounded p-0.5 text-slate-300 opacity-0 hover:bg-red-50 hover:text-red-600 group-hover:opacity-100"
                title="Remove"
              >
                <X className="h-3 w-3" />
              </button>
            </div>
          ))}
        </div>
      )}

      <SkillDialog skill={dialogSkill} open={dialogOpen} onOpenChange={setDialogOpen} />
    </section>
  )
}
