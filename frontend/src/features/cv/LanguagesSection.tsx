import { useState } from 'react'
import { Pencil, Plus, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useDeleteLanguage } from './hooks'
import { LanguageDialog } from './LanguageDialog'
import type { Language } from './types'

interface Props {
  languages: Language[]
}

export function LanguagesSection({ languages }: Props) {
  const [dialogLanguage, setDialogLanguage] = useState<Language | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const deleteLanguage = useDeleteLanguage()

  const openAdd = () => {
    setDialogLanguage(null)
    setDialogOpen(true)
  }
  const openEdit = (language: Language) => {
    setDialogLanguage(language)
    setDialogOpen(true)
  }

  return (
    <section className="border-t border-slate-200 pt-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Languages ({languages.length})</h2>
        <Button variant="outline" size="sm" onClick={openAdd}>
          <Plus className="h-4 w-4" />
          Add
        </Button>
      </div>

      {languages.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">No languages yet. Add languages you speak.</p>
      ) : (
        <div className="mt-3 flex flex-wrap gap-2">
          {languages.map((language) => (
            <div
              key={language.id}
              className="group flex items-center gap-1.5 rounded-full border border-slate-200 bg-white py-1 pl-3 pr-1.5 text-sm text-slate-700"
            >
              <span>
                {language.name}
                {language.level && <span className="text-slate-400"> · {language.level}</span>}
              </span>
              <button
                type="button"
                onClick={() => openEdit(language)}
                className="rounded p-0.5 text-slate-300 opacity-0 hover:bg-slate-100 hover:text-slate-600 group-hover:opacity-100"
                title="Edit"
              >
                <Pencil className="h-3 w-3" />
              </button>
              <button
                type="button"
                onClick={() => deleteLanguage.mutate(language.id)}
                className="rounded p-0.5 text-slate-300 opacity-0 hover:bg-red-50 hover:text-red-600 group-hover:opacity-100"
                title="Remove"
              >
                <X className="h-3 w-3" />
              </button>
            </div>
          ))}
        </div>
      )}

      <LanguageDialog language={dialogLanguage} open={dialogOpen} onOpenChange={setDialogOpen} />
    </section>
  )
}
