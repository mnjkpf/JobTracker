import { useState } from 'react'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useRefineCoverLetter } from '@/features/coverLetters/hooks'

interface Props {
  applicationId: string
  coverLetterId: string | null
  version?: number
  onOpenChange: (open: boolean) => void
}

export function RefineCoverLetterDialog({
  applicationId,
  coverLetterId,
  version,
  onOpenChange,
}: Props) {
  const [instruction, setInstruction] = useState('')
  const refine = useRefineCoverLetter(applicationId, coverLetterId ?? '')

  const submit = () => {
    if (!instruction.trim()) return
    refine.mutate(
      { instruction: instruction.trim() },
      {
        onSuccess: () => {
          setInstruction('')
          onOpenChange(false)
        },
      },
    )
  }

  return (
    <Dialog open={!!coverLetterId} onOpenChange={(o) => !o && onOpenChange(false)}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Refine{version ? ` v${version}` : ''}</DialogTitle>
          <DialogDescription>
            Describe how to improve it — this creates a new version, the original is kept.
          </DialogDescription>
        </DialogHeader>
        <Textarea
          rows={4}
          value={instruction}
          onChange={(e) => setInstruction(e.target.value)}
          placeholder="Make it more concise and remove references to my internship…"
        />
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={submit} disabled={refine.isPending || !instruction.trim()}>
            {refine.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Refine
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
