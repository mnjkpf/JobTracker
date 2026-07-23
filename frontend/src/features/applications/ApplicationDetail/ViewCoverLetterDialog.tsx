import { Loader2 } from 'lucide-react'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { useCoverLetter } from '@/features/coverLetters/hooks'

interface Props {
  applicationId: string
  coverLetterId: string | null
  version?: number
  onOpenChange: (open: boolean) => void
}

export function ViewCoverLetterDialog({ applicationId, coverLetterId, version, onOpenChange }: Props) {
  const { data, isLoading } = useCoverLetter(applicationId, coverLetterId)

  return (
    <Dialog open={!!coverLetterId} onOpenChange={(o) => !o && onOpenChange(false)}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Cover letter{version ? ` · v${version}` : ''}</DialogTitle>
        </DialogHeader>
        {isLoading || !data ? (
          <div className="py-16 text-center">
            <Loader2 className="mx-auto h-6 w-6 animate-spin text-slate-400" />
          </div>
        ) : (
          <div className="max-h-[65vh] overflow-y-auto whitespace-pre-wrap rounded-md border border-slate-200 bg-slate-50 p-4 font-mono text-sm leading-relaxed text-slate-800">
            {data.content}
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
