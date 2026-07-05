import { useDraggable } from '@dnd-kit/core'
import { CSS } from '@dnd-kit/utilities'
import { useNavigate } from 'react-router-dom'
import { ExternalLink, MapPin, Trash2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import type { Application } from './types'

function relativeDate(iso: string | null): string {
  if (!iso) return ''
  const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000)
  if (days <= 0) return 'today'
  if (days === 1) return 'yesterday'
  if (days < 30) return `${days}d ago`
  const months = Math.floor(days / 30)
  return months === 1 ? '1mo ago' : `${months}mo ago`
}

function salaryText(app: Application): string | null {
  if (app.salaryMin == null && app.salaryMax == null) return null
  const cur = app.salaryCurrency ?? ''
  if (app.salaryMin != null && app.salaryMax != null) {
    return `${app.salaryMin.toLocaleString()}–${app.salaryMax.toLocaleString()} ${cur}`.trim()
  }
  const single = (app.salaryMin ?? app.salaryMax) as number
  return `${single.toLocaleString()} ${cur}`.trim()
}

interface Props {
  application: Application
  onDelete: (id: string) => void
}

export function ApplicationCard({ application, onDelete }: Props) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: application.id,
  })
  const salary = salaryText(application)
  const navigate = useNavigate()

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), zIndex: isDragging ? 50 : undefined }}
      {...listeners}
      {...attributes}
      onClick={() => navigate(`/applications/${application.id}`)}
      className={cn(
        'group relative touch-none cursor-grab rounded-md border border-slate-200 bg-white p-3 shadow-sm transition-shadow hover:shadow-md active:cursor-grabbing',
        isDragging && 'opacity-60 shadow-lg',
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <p className="text-sm font-semibold leading-snug text-slate-900">{application.name}</p>
        <a
          href={application.url}
          target="_blank"
          rel="noreferrer"
          onPointerDown={(e) => e.stopPropagation()}
          onClick={(e) => e.stopPropagation()}
          className="mt-0.5 shrink-0 text-slate-400 hover:text-slate-700"
          title="Open job posting"
        >
          <ExternalLink className="h-3.5 w-3.5" />
        </a>
      </div>

      {application.companyName && (
        <p className="mt-0.5 text-xs text-slate-500">{application.companyName}</p>
      )}

      <div className="mt-2 flex flex-wrap gap-1">
        {application.location && (
          <Badge variant="outline" className="gap-1 text-slate-600">
            <MapPin className="h-3 w-3" />
            {application.location}
          </Badge>
        )}
        <Badge variant="outline" className="text-slate-600">
          {application.workMode.toLowerCase()}
        </Badge>
        <Badge variant="outline" className="text-slate-600">
          {application.contractType}
        </Badge>
      </div>

      {salary && <p className="mt-2 text-xs font-medium text-slate-700">{salary}</p>}

      <div className="mt-2 flex items-center justify-between">
        <span className="text-[11px] text-slate-400">
          {relativeDate(application.appliedAt || application.createdAt)}
        </span>
        <button
          type="button"
          onPointerDown={(e) => e.stopPropagation()}
          onClick={(e) => {
            e.stopPropagation()
            onDelete(application.id)
          }}
          className="text-slate-300 opacity-0 transition-opacity hover:text-red-500 group-hover:opacity-100"
          title="Delete application"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  )
}
