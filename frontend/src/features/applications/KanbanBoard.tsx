import { useMemo, useState } from 'react'
import { DndContext, PointerSensor, closestCorners, useSensor, useSensors } from '@dnd-kit/core'
import type { DragEndEvent } from '@dnd-kit/core'
import { toast } from 'sonner'
import { Plus, RotateCw, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import { useDebounce } from '@/lib/hooks/useDebounce'
import { KanbanColumn } from './KanbanColumn'
import { CreateApplicationModal } from './CreateApplicationModal'
import { KANBAN_COLUMNS } from './statusMeta'
import { useApplications, useDeleteApplication, useUpdateStatus } from './hooks'
import { isValidTransition } from './types'
import type { Application, ApplicationStatus } from './types'

type FilterKey = 'all' | 'active' | 'interviews' | 'rejected'

const FILTERS: { key: FilterKey; label: string; statuses?: ApplicationStatus[] }[] = [
  { key: 'all', label: 'All' },
  { key: 'active', label: 'Active only', statuses: ['APPLIED', 'SCREENING', 'INTERVIEW', 'FINAL'] },
  { key: 'interviews', label: 'Interviews', statuses: ['INTERVIEW', 'FINAL'] },
  { key: 'rejected', label: 'Rejected', statuses: ['REJECTED'] },
]

function BoardSkeleton() {
  return (
    <div className="flex gap-3 overflow-x-auto pb-4">
      {KANBAN_COLUMNS.map((col) => (
        <div key={col.status} className="w-72 shrink-0 rounded-lg bg-slate-100 p-2">
          <div className="mb-2 h-5 w-24 animate-pulse rounded bg-slate-200" />
          <div className="space-y-2">
            {[0, 1, 2].map((i) => (
              <div key={i} className="h-20 animate-pulse rounded-md bg-slate-200" />
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

function ErrorBanner({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-red-200 bg-red-50 px-4 py-3">
      <p className="text-sm text-red-700">Couldn&apos;t load applications. Is the backend running?</p>
      <Button variant="outline" size="sm" onClick={onRetry}>
        Retry
      </Button>
    </div>
  )
}

function EmptyState({ onCreate }: { onCreate: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-20 text-center">
      <h3 className="text-lg font-semibold text-slate-900">No applications yet</h3>
      <p className="mt-1 max-w-sm text-sm text-slate-500">
        Add the first job you&apos;re applying to and start tracking it across the pipeline.
      </p>
      <Button className="mt-4" onClick={onCreate}>
        <Plus className="h-4 w-4" />
        Create your first application
      </Button>
    </div>
  )
}

function NoMatchesState() {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-20 text-center">
      <h3 className="text-lg font-semibold text-slate-900">No matches</h3>
      <p className="mt-1 max-w-sm text-sm text-slate-500">
        No applications match your search or filter. Try adjusting them.
      </p>
    </div>
  )
}

export function KanbanBoard() {
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<FilterKey>('all')
  const debouncedSearch = useDebounce(search, 300)
  const activeFilter = FILTERS.find((f) => f.key === filter)

  const { data, isLoading, isError, isFetching, refetch } = useApplications({
    q: debouncedSearch || undefined,
    statuses: activeFilter?.statuses,
  })
  const updateStatus = useUpdateStatus()
  const deleteApp = useDeleteApplication()
  const [createOpen, setCreateOpen] = useState(false)

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }))

  const applications = data?.content ?? []
  const isFiltered = debouncedSearch.length > 0 || filter !== 'all'

  const byStatus = useMemo(() => {
    const map = new Map<ApplicationStatus, Application[]>()
    for (const col of KANBAN_COLUMNS) map.set(col.status, [])
    for (const app of applications) {
      map.get(app.status)?.push(app)
    }
    return map
  }, [applications])

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    if (!over) return
    const app = applications.find((a) => a.id === String(active.id))
    if (!app) return

    const target = String(over.id) as ApplicationStatus
    if (app.status === target) return

    if (!isValidTransition(app.status, target)) {
      toast.error(`Cannot move from ${app.status} to ${target}`)
      return
    }
    updateStatus.mutate({ id: app.id, data: { status: target } })
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-2xl font-bold tracking-tight text-slate-900">Applications</h2>
          {isFetching && !isLoading && <RotateCw className="h-4 w-4 animate-spin text-slate-400" />}
        </div>
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              // Backend ApplicationFilters.q only searches name + description, not companyName.
              placeholder="Search by position or description..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-64 pl-8"
            />
          </div>
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" />
            New Application
          </Button>
        </div>
      </div>

      <div className="mb-6 flex items-center gap-2">
        <span className="text-sm text-slate-500">Filters:</span>
        {FILTERS.map((f) => (
          <button
            key={f.key}
            type="button"
            onClick={() => setFilter(f.key)}
            className={cn(
              'rounded-full border px-3 py-1 text-xs font-medium transition-colors',
              filter === f.key
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-100',
            )}
          >
            {f.label}
          </button>
        ))}
        {!isLoading && !isError && (
          <span className="ml-1 text-xs text-slate-400">{data?.totalElements ?? 0} applications</span>
        )}
      </div>

      {isLoading ? (
        <BoardSkeleton />
      ) : isError ? (
        <ErrorBanner onRetry={() => refetch()} />
      ) : applications.length === 0 ? (
        isFiltered ? <NoMatchesState /> : <EmptyState onCreate={() => setCreateOpen(true)} />
      ) : (
        <DndContext
          sensors={sensors}
          collisionDetection={closestCorners}
          onDragEnd={handleDragEnd}
        >
          <div className="flex gap-3 overflow-x-auto pb-4">
            {KANBAN_COLUMNS.map((col) => (
              <KanbanColumn
                key={col.status}
                status={col.status}
                label={col.label}
                applications={byStatus.get(col.status) ?? []}
                onDelete={(id) => deleteApp.mutate(id)}
              />
            ))}
          </div>
        </DndContext>
      )}

      <CreateApplicationModal open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  )
}
