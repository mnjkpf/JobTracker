import { useState } from 'react'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useCreateNote } from '@/features/interviewPrep/hooks'
import type { NoteType } from '@/features/interviewPrep/api'

const NOTE_TYPES: { value: NoteType; label: string }[] = [
  { value: 'PREP_NOTE', label: 'Prep note' },
  { value: 'POST_INTERVIEW', label: 'Post-interview' },
]

interface Props {
  applicationId: string
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function AddNoteDialog({ applicationId, open, onOpenChange }: Props) {
  const [content, setContent] = useState('')
  const [noteType, setNoteType] = useState<NoteType>('PREP_NOTE')
  const create = useCreateNote(applicationId)

  const submit = () => {
    if (!content.trim()) return
    create.mutate(
      { content: content.trim(), noteType },
      {
        onSuccess: () => {
          setContent('')
          setNoteType('PREP_NOTE')
          onOpenChange(false)
        },
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Add note</DialogTitle>
          <DialogDescription>
            Notes are embedded and used by RAG to inform future prep guides for similar interviews.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label>Type</Label>
            <Select value={noteType} onValueChange={(v) => setNoteType(v as NoteType)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {NOTE_TYPES.map((t) => (
                  <SelectItem key={t.value} value={t.value}>
                    {t.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label>Content</Label>
            <Textarea
              autoFocus
              rows={5}
              maxLength={5000}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Need to review Spring @Transactional propagation levels…"
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={submit} disabled={create.isPending || !content.trim()}>
            {create.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Save note
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
