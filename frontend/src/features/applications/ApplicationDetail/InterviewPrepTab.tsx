import { useState } from 'react'
import { Loader2, RefreshCw, Sparkles } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useGeneratePrep, useInterviewPrep, useRelevantNotes } from '@/features/interviewPrep/hooks'
import { useUpdateStatus } from '../hooks'
import { STATUS_META } from '../statusMeta'
import { VALID_TRANSITIONS } from '../types'
import type { ApplicationStatus } from '../types'
import { PrepGuidePanel } from './PrepGuidePanel'
import { NotesPanel } from './NotesPanel'
import { RagInsightsPanel } from './RagInsightsPanel'

function statusOf(error: unknown): number | undefined {
  return (error as { response?: { status?: number } })?.response?.status
}

function fmt(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function NotInterviewYet({ applicationId, status }: { applicationId: string; status: ApplicationStatus }) {
  const updateStatus = useUpdateStatus()
  const canMoveToInterview = VALID_TRANSITIONS[status].includes('INTERVIEW')

  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-16 text-center">
      <Sparkles className="mb-3 h-8 w-8 text-slate-400" />
      <h3 className="text-lg font-semibold text-slate-900">Interview prep</h3>
      <p className="mt-1 max-w-md text-sm text-slate-500">
        Move this application to Interview status to unlock an AI-generated prep guide — tailored
        questions, suggested answers, and insights pulled from your past interview notes.
      </p>
      {canMoveToInterview ? (
        <Button
          className="mt-4"
          disabled={updateStatus.isPending}
          onClick={() => updateStatus.mutate({ id: applicationId, data: { status: 'INTERVIEW' } })}
        >
          {updateStatus.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
          Move to Interview
        </Button>
      ) : (
        <p className="mt-3 text-xs text-slate-400">
          Current status: {STATUS_META[status].label} — progress this application to Interview from
          the Overview tab.
        </p>
      )}
    </div>
  )
}

export function InterviewPrepTab({
  applicationId,
  status,
}: {
  applicationId: string
  status: ApplicationStatus
}) {
  const { data: prep, isLoading, error } = useInterviewPrep(applicationId)
  const generate = useGeneratePrep(applicationId)
  const relevantNotes = useRelevantNotes(applicationId)
  const [confirmOpen, setConfirmOpen] = useState(false)

  if (isLoading) {
    return (
      <div className="space-y-3">
        <div className="h-8 w-64 animate-pulse rounded bg-slate-100" />
        <div className="h-40 animate-pulse rounded-lg bg-slate-100" />
      </div>
    )
  }

  if (!prep || statusOf(error) === 404) {
    return <NotInterviewYet applicationId={applicationId} status={status} />
  }

  const relevantCount = relevantNotes.data?.length ?? 0

  const handleGenerate = () => {
    generate.mutate(undefined, { onSuccess: () => setConfirmOpen(false) })
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Interview Prep</h2>
          <p className="text-sm text-slate-500">
            {prep.status === 'GENERATED'
              ? `Generated${prep.promptVersion ? ` · ${prep.promptVersion}` : ''} · ${fmt(prep.createdAt)}`
              : 'Guide not generated yet'}
          </p>
        </div>
        {prep.status === 'GENERATED' ? (
          <Button variant="outline" size="sm" onClick={() => setConfirmOpen(true)} disabled={generate.isPending}>
            <RefreshCw className="h-4 w-4" /> Regenerate
          </Button>
        ) : (
          <Button size="sm" onClick={handleGenerate} disabled={generate.isPending}>
            {generate.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
            Generate prep guide
          </Button>
        )}
      </div>

      {generate.isPending && (
        <div className="flex items-center gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
          <Loader2 className="h-4 w-4 shrink-0 animate-spin text-slate-400" />
          <span>
            Analyzing job requirements, your CV
            {relevantCount > 0
              ? `, and ${relevantCount} relevant past interview note${relevantCount === 1 ? '' : 's'}`
              : ''}
            … this takes about 10–20 seconds.
          </span>
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <PrepGuidePanel prep={prep} />
        <NotesPanel applicationId={applicationId} />
      </div>

      <RagInsightsPanel applicationId={applicationId} />

      <Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Regenerate prep guide?</DialogTitle>
            <DialogDescription>
              This replaces all AI-generated questions with new ones based on the latest RAG
              insights. Your notes are preserved.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleGenerate} disabled={generate.isPending}>
              {generate.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Regenerate
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
