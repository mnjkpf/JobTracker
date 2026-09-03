import { useMemo, useState } from 'react'
import { DndContext, PointerSensor, closestCorners, useSensor, useSensors } from '@dnd-kit/core'
import type { DragEndEvent } from '@dnd-kit/core'
import { Plus, RotateCw, Search, SlidersHorizontal } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import { useDebounce } from '@/lib/hooks/useDebounce'
import { useStatuses } from '@/features/statuses/hooks'
import { StatusManagerDialog } from '@/features/statuses/StatusManagerDialog'
import { KanbanColumn } from './KanbanColumn'
import { CreateApplicationModal } from './CreateApplicationModal'
import { ArchivedApplicationsList } from './ArchivedApplicationsList'
import { useApplications, useArchiveApplication, useUpdateStatus } from './hooks'
import type { Application } from './types'

type View = 'active' | 'archived'

function BoardSkeleton() {
  return (
    <div className="flex gap-3 overflow-x-auto pb-4">
      {Array.from({ length: 5 }).map((_, c) => (
        <div key={c} className="w-72 shrink-0 rounded-lg bg-slate-100 p-2">
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
        No applications match your search. Try adjusting it.
      </p>
    </div>
  )
}

export function KanbanBoard() {
  const [search, setSearch] = useState('')
  const [view, setView] = useState<View>('active')
  const [createOpen, setCreateOpen] = useState(false)
  const [manageOpen, setManageOpen] = useState(false)
  const debouncedSearch = useDebounce(search, 300)

  const statusesQuery = useStatuses()
  const { data, isLoading, isError, isFetching, refetch } = useApplications({
    q: debouncedSearch || undefined,
    archived: view === 'archived' ? true : undefined,
  })
  const updateStatus = useUpdateStatus()
  const archiveApp = useArchiveApplication()

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }))

  const columns = useMemo(
    () => (statusesQuery.data ?? []).slice().sort((a, b) => a.position - b.position),
    [statusesQuery.data],
  )
  const applications = data?.content ?? []
  const isSearching = debouncedSearch.length > 0

  const byStatus = useMemo(() => {
    const map = new Map<string, Application[]>()
    for (const col of columns) map.set(col.id, [])
    for (const app of applications) {
      if (map.has(app.status.id)) map.get(app.status.id)!.push(app)
    }
    return map
  }, [applications, columns])

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    if (!over) return
    const app = applications.find((a) => a.id === String(active.id))
    if (!app) return
    const targetStatusId = String(over.id)
    if (app.status.id === targetStatusId) return
    // Free movement — any column to any column.
    updateStatus.mutate({ id: app.id, data: { statusId: targetStatusId } })
  }

  const boardLoading = isLoading || statusesQuery.isLoading

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
              placeholder="Search by position or description..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-64 pl-8"
            />
          </div>
          <Button variant="outline" onClick={() => setManageOpen(true)}>
            <SlidersHorizontal className="h-4 w-4" />
            Statuses
          </Button>
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" />
            New Application
          </Button>
        </div>
      </div>

      <div className="mb-6 flex items-center gap-2">
        {(['active', 'archived'] as const).map((v) => (
          <button
            key={v}
            type="button"
            onClick={() => setView(v)}
            className={cn(
              'rounded-full border px-3 py-1 text-xs font-medium capitalize transition-colors',
              view === v
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-100',
            )}
          >
            {v}
          </button>
        ))}
        {!boardLoading && !isError && (
          <span className="ml-1 text-xs text-slate-400">{data?.totalElements ?? 0} applications</span>
        )}
      </div>

      {view === 'archived' ? (
        boardLoading ? (
          <div className="space-y-2">
            {[0, 1, 2].map((i) => (
              <div key={i} className="h-16 animate-pulse rounded-md bg-slate-100" />
            ))}
          </div>
        ) : isError ? (
          <ErrorBanner onRetry={() => refetch()} />
        ) : (
          <ArchivedApplicationsList applications={applications} />
        )
      ) : boardLoading ? (
        <BoardSkeleton />
      ) : isError ? (
        <ErrorBanner onRetry={() => refetch()} />
      ) : applications.length === 0 ? (
        isSearching ? <NoMatchesState /> : <EmptyState onCreate={() => setCreateOpen(true)} />
      ) : (
        <DndContext sensors={sensors} collisionDetection={closestCorners} onDragEnd={handleDragEnd}>
          <div className="flex gap-3 overflow-x-auto pb-4">
            {columns.map((col) => (
              <KanbanColumn
                key={col.id}
                status={col}
                applications={byStatus.get(col.id) ?? []}
                onArchive={(id) => archiveApp.mutate(id)}
              />
            ))}
          </div>
        </DndContext>
      )}

      <CreateApplicationModal open={createOpen} onOpenChange={setCreateOpen} />
      <StatusManagerDialog open={manageOpen} onOpenChange={setManageOpen} />
    </div>
  )
}
