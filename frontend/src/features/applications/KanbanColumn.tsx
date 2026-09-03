import { useDroppable } from '@dnd-kit/core'
import { cn } from '@/lib/utils'
import { ApplicationCard } from './ApplicationCard'
import { statusBadgeStyle, statusDotStyle } from './statusMeta'
import type { Application } from './types'
import type { Status } from '@/features/statuses/api'

interface Props {
  status: Status
  applications: Application[]
  onArchive: (id: string) => void
}

export function KanbanColumn({ status, applications, onArchive }: Props) {
  const { setNodeRef, isOver } = useDroppable({ id: status.id })

  return (
    <div className="flex w-72 shrink-0 flex-col rounded-lg bg-slate-100">
      <div className="flex items-center gap-2 px-3 py-2.5">
        <span className="h-2 w-2 rounded-full" style={statusDotStyle(status.color)} />
        <span className="text-sm font-medium text-slate-700">{status.name}</span>
        <span
          className="ml-auto rounded-full px-2 py-0.5 text-xs font-medium"
          style={statusBadgeStyle(status.color)}
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
            <ApplicationCard key={app.id} application={app} onArchive={onArchive} />
          ))
        )}
      </div>
    </div>
  )
}
