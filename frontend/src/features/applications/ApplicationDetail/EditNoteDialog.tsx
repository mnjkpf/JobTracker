import { useEffect, useState } from 'react'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useUpdateNote } from '@/features/interviewPrep/hooks'
import type { InterviewNote } from '@/features/interviewPrep/api'

interface Props {
  applicationId: string
  note: InterviewNote | null
  onOpenChange: (open: boolean) => void
}

export function EditNoteDialog({ applicationId, note, onOpenChange }: Props) {
  const [content, setContent] = useState('')
  const update = useUpdateNote(applicationId)

  useEffect(() => {
    if (note) setContent(note.content)
  }, [note])

  const submit = () => {
    if (!note || !content.trim()) return
    update.mutate(
      { noteId: note.id, data: { content: content.trim() } },
      { onSuccess: () => onOpenChange(false) },
    )
  }

  return (
    <Dialog open={!!note} onOpenChange={(o) => !o && onOpenChange(false)}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Edit note</DialogTitle>
          <DialogDescription>
            {note?.noteType === 'POST_INTERVIEW' ? 'Post-interview reflection' : 'Prep note'} — type
            can't be changed.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-1.5">
          <Label>Content</Label>
          <Textarea
            autoFocus
            rows={5}
            maxLength={5000}
            value={content}
            onChange={(e) => setContent(e.target.value)}
          />
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={submit} disabled={update.isPending || !content.trim()}>
            {update.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Save
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
