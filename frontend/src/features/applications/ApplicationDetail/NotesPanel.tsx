import { useState } from 'react'
import { Loader2, MessageSquarePlus, Pencil, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { cn } from '@/lib/utils'
import { useDeleteNote, useInterviewNotes } from '@/features/interviewPrep/hooks'
import type { InterviewNote } from '@/features/interviewPrep/api'
import { AddNoteDialog } from './AddNoteDialog'
import { EditNoteDialog } from './EditNoteDialog'
import { AddReflectionDialog } from './AddReflectionDialog'

function fmt(iso: string) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

function NoteCard({
  note,
  onEdit,
  onDelete,
}: {
  note: InterviewNote
  onEdit: () => void
  onDelete: () => void
}) {
  const isReflection = note.noteType === 'POST_INTERVIEW'
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <span
        className={cn(
          'rounded-full px-2 py-0.5 text-xs font-medium',
          isReflection ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700',
        )}
      >
        {isReflection ? 'Post-interview' : 'Prep note'}
      </span>
      <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-slate-700">{note.content}</p>
      <div className="mt-3 flex items-center justify-between">
        <span className="text-xs text-slate-400">{fmt(note.createdAt)}</span>
        <div className="flex gap-1">
          <Button variant="ghost" size="sm" onClick={onEdit}>
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          <Button variant="ghost" size="sm" className="text-red-600 hover:text-red-700" onClick={onDelete}>
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>
    </div>
  )
}

export function NotesPanel({ applicationId }: { applicationId: string }) {
  const { data, isLoading } = useInterviewNotes(applicationId)
  const del = useDeleteNote(applicationId)

  const [addOpen, setAddOpen] = useState(false)
  const [reflectionOpen, setReflectionOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<InterviewNote | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<InterviewNote | null>(null)

  const notes = data ?? []

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-900">Notes ({notes.length})</h3>
        <Button size="sm" variant="outline" onClick={() => setAddOpen(true)}>
          <Plus className="h-3.5 w-3.5" /> Add note
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1].map((i) => (
            <div key={i} className="h-20 animate-pulse rounded-lg bg-slate-100" />
          ))}
        </div>
      ) : notes.length === 0 ? (
        <div className="rounded-lg border border-dashed border-slate-300 bg-white py-10 text-center text-sm text-slate-500">
          No notes yet. Jot down things to review before the interview.
        </div>
      ) : (
        <div className="space-y-3">
          {notes.map((n) => (
            <NoteCard key={n.id} note={n} onEdit={() => setEditTarget(n)} onDelete={() => setDeleteTarget(n)} />
          ))}
        </div>
      )}

      <Button variant="secondary" size="sm" className="w-full" onClick={() => setReflectionOpen(true)}>
        <MessageSquarePlus className="h-4 w-4" /> Add post-interview reflection
      </Button>

      <AddNoteDialog applicationId={applicationId} open={addOpen} onOpenChange={setAddOpen} />
      <EditNoteDialog
        applicationId={applicationId}
        note={editTarget}
        onOpenChange={(o) => !o && setEditTarget(null)}
      />
      <AddReflectionDialog applicationId={applicationId} open={reflectionOpen} onOpenChange={setReflectionOpen} />

      <Dialog open={!!deleteTarget} onOpenChange={(o) => !o && setDeleteTarget(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Delete note</DialogTitle>
            <DialogDescription>This cannot be undone.</DialogDescription>
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
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
