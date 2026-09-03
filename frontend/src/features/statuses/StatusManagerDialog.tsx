import { useState } from 'react'
import { ChevronDown, ChevronUp, Loader2, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { cn } from '@/lib/utils'
import { PRESET_COLORS } from './colors'
import {
  useCreateStatus,
  useDeleteStatus,
  useReorderStatuses,
  useStatuses,
  useUpdateStatusCategory,
} from './hooks'
import type { Status } from './api'

function ColorSwatches({
  value,
  onPick,
}: {
  value: string
  onPick: (hex: string) => void
}) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {PRESET_COLORS.map((c) => (
        <button
          key={c}
          type="button"
          onClick={() => onPick(c)}
          style={{ backgroundColor: c }}
          className={cn(
            'h-6 w-6 rounded-full ring-offset-2 transition-transform hover:scale-110',
            value.toLowerCase() === c.toLowerCase() && 'ring-2 ring-slate-900',
          )}
          aria-label={c}
        />
      ))}
    </div>
  )
}

function StatusRow({
  status,
  index,
  total,
  statuses,
  busy,
  onMove,
}: {
  status: Status
  index: number
  total: number
  statuses: Status[]
  busy: boolean
  onMove: (from: number, to: number) => void
}) {
  const update = useUpdateStatusCategory()
  const del = useDeleteStatus()
  const [paletteOpen, setPaletteOpen] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [reassignTo, setReassignTo] = useState<string>('')

  const others = statuses.filter((s) => s.id !== status.id)

  const saveName = (name: string) => {
    const trimmed = name.trim()
    if (trimmed && trimmed !== status.name) {
      update.mutate({ id: status.id, data: { name: trimmed } })
    }
  }

  return (
    <div className="rounded-md border border-slate-200 bg-white p-2.5">
      <div className="flex items-center gap-2">
        <div className="flex flex-col">
          <button
            type="button"
            disabled={index === 0 || busy}
            onClick={() => onMove(index, index - 1)}
            className="text-slate-400 hover:text-slate-700 disabled:opacity-30"
          >
            <ChevronUp className="h-3.5 w-3.5" />
          </button>
          <button
            type="button"
            disabled={index === total - 1 || busy}
            onClick={() => onMove(index, index + 1)}
            className="text-slate-400 hover:text-slate-700 disabled:opacity-30"
          >
            <ChevronDown className="h-3.5 w-3.5" />
          </button>
        </div>

        <button
          type="button"
          onClick={() => setPaletteOpen((o) => !o)}
          style={{ backgroundColor: status.color }}
          className="h-6 w-6 shrink-0 rounded-full ring-1 ring-inset ring-black/10"
          title="Change color"
        />

        <Input
          defaultValue={status.name}
          onBlur={(e) => saveName(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') (e.target as HTMLInputElement).blur()
          }}
          className="h-8 flex-1"
        />

        {status.systemType && (
          <span
            className="shrink-0 rounded px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-slate-400"
            title="Built-in status — drives applied date / interview-prep automation"
          >
            {status.systemType.toLowerCase()}
          </span>
        )}

        <button
          type="button"
          disabled={busy}
          onClick={() => setConfirmDelete((c) => !c)}
          className="shrink-0 text-slate-300 transition-colors hover:text-red-500"
          title="Delete status"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>

      {paletteOpen && (
        <div className="mt-2 pl-8">
          <ColorSwatches
            value={status.color}
            onPick={(hex) => {
              update.mutate({ id: status.id, data: { color: hex } })
              setPaletteOpen(false)
            }}
          />
        </div>
      )}

      {confirmDelete && (
        <div className="mt-2 space-y-2 rounded-md bg-slate-50 p-2.5 pl-8">
          <p className="text-xs text-slate-600">
            Delete <span className="font-medium">{status.name}</span>? If it has applications,
            choose where to move them:
          </p>
          <div className="flex items-center gap-2">
            <Select value={reassignTo} onValueChange={setReassignTo}>
              <SelectTrigger className="h-8 flex-1">
                <SelectValue placeholder="Move applications to… (optional)" />
              </SelectTrigger>
              <SelectContent>
                {others.map((s) => (
                  <SelectItem key={s.id} value={s.id}>
                    {s.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button
              variant="destructive"
              size="sm"
              disabled={del.isPending}
              onClick={() =>
                del.mutate(
                  { id: status.id, reassignTo: reassignTo || undefined },
                  { onSuccess: () => setConfirmDelete(false) },
                )
              }
            >
              {del.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Delete
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setConfirmDelete(false)}>
              Cancel
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}

export function StatusManagerDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { data: statuses } = useStatuses()
  const create = useCreateStatus()
  const reorder = useReorderStatuses()

  const [newName, setNewName] = useState('')
  const [newColor, setNewColor] = useState(PRESET_COLORS[0])

  const list = statuses ?? []
  const busy = reorder.isPending

  const move = (from: number, to: number) => {
    if (to < 0 || to >= list.length) return
    const ids = list.map((s) => s.id)
    const [moved] = ids.splice(from, 1)
    ids.splice(to, 0, moved)
    reorder.mutate(ids)
  }

  const addStatus = () => {
    const name = newName.trim()
    if (!name) return
    create.mutate(
      { name, color: newColor },
      { onSuccess: () => setNewName('') },
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Manage statuses</DialogTitle>
          <DialogDescription>
            Add, rename, recolor, reorder or remove board columns. Built-in statuses keep their
            automations (applied date, interview prep).
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2">
          {list.map((s, i) => (
            <StatusRow
              key={s.id}
              status={s}
              index={i}
              total={list.length}
              statuses={list}
              busy={busy}
              onMove={move}
            />
          ))}
        </div>

        <div className="mt-3 space-y-2 rounded-md border border-dashed border-slate-300 p-3">
          <p className="text-xs font-medium text-slate-500">New status</p>
          <div className="flex items-center gap-2">
            <span
              style={{ backgroundColor: newColor }}
              className="h-6 w-6 shrink-0 rounded-full ring-1 ring-inset ring-black/10"
            />
            <Input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && addStatus()}
              placeholder="e.g. Take-home task"
              className="h-8 flex-1"
            />
            <Button size="sm" onClick={addStatus} disabled={create.isPending || !newName.trim()}>
              {create.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}
              Add
            </Button>
          </div>
          <ColorSwatches value={newColor} onPick={setNewColor} />
        </div>
      </DialogContent>
    </Dialog>
  )
}
