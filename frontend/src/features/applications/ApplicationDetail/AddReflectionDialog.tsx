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
import { useAddReflection } from '@/features/interviewPrep/hooks'

interface Props {
  applicationId: string
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function AddReflectionDialog({ applicationId, open, onOpenChange }: Props) {
  const [content, setContent] = useState('')
  const [questionsText, setQuestionsText] = useState('')
  const addReflection = useAddReflection(applicationId)

  const submit = () => {
    if (!content.trim()) return
    const questionsAsked = questionsText
      .split('\n')
      .map((q) => q.trim())
      .filter(Boolean)

    addReflection.mutate(
      {
        content: content.trim(),
        questionsAsked: questionsAsked.length > 0 ? questionsAsked : undefined,
      },
      {
        onSuccess: () => {
          setContent('')
          setQuestionsText('')
          onOpenChange(false)
        },
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Add post-interview reflection</DialogTitle>
          <DialogDescription>
            How did it go? This becomes a post-interview note — future prep guides for similar
            interviews will learn from it.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label>Your reflection</Label>
            <Textarea
              autoFocus
              rows={5}
              maxLength={10000}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="How did the interview go? What did you learn?"
            />
          </div>
          <div className="space-y-1.5">
            <Label>Questions asked (one per line)</Label>
            <Textarea
              rows={4}
              value={questionsText}
              onChange={(e) => setQuestionsText(e.target.value)}
              placeholder={'Tell me about yourself\nExplain @Transactional\nWhat is the N+1 problem'}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={submit} disabled={addReflection.isPending || !content.trim()}>
            {addReflection.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Save reflection
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
