import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Loader2, RotateCcw, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { statusBadgeStyle } from './statusMeta'
import { useDeleteApplication, useUnarchiveApplication } from './hooks'
import type { Application } from './types'

export function ArchivedApplicationsList({ applications }: { applications: Application[] }) {
  const navigate = useNavigate()
  const unarchive = useUnarchiveApplication()
  const del = useDeleteApplication()
  const [deleteTarget, setDeleteTarget] = useState<Application | null>(null)

  if (applications.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-20 text-center">
        <h3 className="text-lg font-semibold text-slate-900">Archive is empty</h3>
        <p className="mt-1 max-w-sm text-sm text-slate-500">
          Applications you archive from the board show up here. You can restore or permanently
          delete them.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      {applications.map((app) => (
        <div
          key={app.id}
          className="flex items-center gap-3 rounded-md border border-slate-200 bg-white p-3"
        >
          <div className="min-w-0 flex-1">
            <button
              type="button"
              onClick={() => navigate(`/applications/${app.id}`)}
              className="truncate text-sm font-medium text-slate-900 hover:underline"
            >
              {app.name}
            </button>
            <p className="truncate text-xs text-slate-500">
              {[app.companyName, app.location].filter(Boolean).join(' · ') || '—'}
            </p>
          </div>

          <span
            className="shrink-0 rounded-full px-2 py-0.5 text-xs font-medium"
            style={statusBadgeStyle(app.status.color)}
          >
            {app.status.name}
          </span>

          <Button
            variant="outline"
            size="sm"
            disabled={unarchive.isPending}
            onClick={() => unarchive.mutate(app.id)}
          >
            <RotateCcw className="h-4 w-4" /> Restore
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="text-red-600 hover:text-red-700"
            onClick={() => setDeleteTarget(app)}
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      ))}

      <Dialog open={!!deleteTarget} onOpenChange={(o) => !o && setDeleteTarget(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Delete permanently</DialogTitle>
            <DialogDescription>
              Permanently delete {deleteTarget?.name}? This also removes its notes, cover letters,
              tailored CVs and interview prep. This cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={del.isPending}
              onClick={() => {
                if (deleteTarget) {
                  del.mutate(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
                }
              }}
            >
              {del.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Delete permanently
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
