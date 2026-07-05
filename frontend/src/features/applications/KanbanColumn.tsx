import { useDroppable } from '@dnd-kit/core'
import { cn } from '@/lib/utils'
import { ApplicationCard } from './ApplicationCard'
import { STATUS_STYLES } from './statusMeta'
import type { Application, ApplicationStatus } from './types'

interface Props {
  status: ApplicationStatus
  label: string
  applications: Application[]
  onDelete: (id: string) => void
}

export function KanbanColumn({ status, label, applications, onDelete }: Props) {
  const { setNodeRef, isOver } = useDroppable({ id: status })
  const style = STATUS_STYLES[status]

  return (
    <div className="flex w-72 shrink-0 flex-col rounded-lg bg-slate-100">
      <div className="flex items-center gap-2 px-3 py-2.5">
        <span className={cn('h-2 w-2 rounded-full', style.dot)} />
        <span className="text-sm font-medium text-slate-700">{label}</span>
        <span
          className={cn(
            'ml-auto rounded-full px-2 py-0.5 text-xs font-medium',
            style.badge,
          )}
        >
          {applications.length}
        </span>
      </div>

      <div
        ref={setNodeRef}
        className={cn(
          'flex min-h-[140px] flex-1 flex-col gap-2 rounded-b-lg p-2 transition-colors',
          isOver && 'bg-slate-200',
        )}
      >
        {applications.length === 0 ? (
          <p className="px-2 py-8 text-center text-xs text-slate-400">Empty</p>
        ) : (
          applications.map((app) => (
            <ApplicationCard key={app.id} application={app} onDelete={onDelete} />
          ))
        )}
      </div>
    </div>
  )
}
