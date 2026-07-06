import { useState } from 'react'
import { Eye, Loader2, Sparkles, Trash2, Wand2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  useCoverLetters,
  useDeleteCoverLetter,
  useGenerateCoverLetter,
} from '@/features/coverLetters/hooks'
import type { CoverLetterSummary, CoverLetterTone } from '@/features/coverLetters/api'
import { ViewCoverLetterDialog } from './ViewCoverLetterDialog'
import { RefineCoverLetterDialog } from './RefineCoverLetterDialog'

const TONES: { value: CoverLetterTone; label: string }[] = [
  { value: 'FORMAL', label: 'Formal' },
  { value: 'CONVERSATIONAL', label: 'Conversational' },
  { value: 'ENTHUSIASTIC', label: 'Enthusiastic' },
]

const toneLabel = (t: CoverLetterTone) => TONES.find((x) => x.value === t)?.label ?? t

function fmt(iso: string) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export function CoverLettersTab({ applicationId }: { applicationId: string }) {
  const { data, isLoading } = useCoverLetters(applicationId)
  const generate = useGenerateCoverLetter(applicationId)
  const del = useDeleteCoverLetter(applicationId)

  const [tone, setTone] = useState<CoverLetterTone>('FORMAL')
  const [viewTarget, setViewTarget] = useState<CoverLetterSummary | null>(null)
  const [refineTarget, setRefineTarget] = useState<CoverLetterSummary | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CoverLetterSummary | null>(null)

  const letters = data ?? []

  const generateBar = (
    <div className="flex items-center gap-2">
      <Select value={tone} onValueChange={(v) => setTone(v as CoverLetterTone)}>
        <SelectTrigger className="w-44">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {TONES.map((t) => (
            <SelectItem key={t.value} value={t.value}>
              {t.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Button onClick={() => generate.mutate({ tone })} disabled={generate.isPending}>
        {generate.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
        Generate
      </Button>
    </div>
  )

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">
          {generate.isPending ? 'Generating cover letter… about 10–15 seconds' : `${letters.length} version${letters.length === 1 ? '' : 's'}`}
        </p>
        {generateBar}
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1].map((i) => (
            <div key={i} className="h-24 animate-pulse rounded-lg bg-slate-100" />
          ))}
        </div>
      ) : letters.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-16 text-center">
          <Sparkles className="mb-3 h-8 w-8 text-slate-400" />
          <h3 className="text-lg font-semibold text-slate-900">No cover letters yet</h3>
          <p className="mt-1 max-w-sm text-sm text-slate-500">
            Generate one tailored to this job — pick a tone above and click Generate.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {letters.map((cl) => (
            <div
              key={cl.id}
              className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-shadow hover:shadow-md"
            >
              <div className="flex items-center justify-between gap-3">
                <div className="text-sm font-medium text-slate-900">
                  Version {cl.version} · <span className="text-slate-500">{toneLabel(cl.tone)}</span>
                </div>
                <span className="text-xs text-slate-400">{fmt(cl.createdAt)}</span>
              </div>
              <p className="mt-2 line-clamp-3 text-sm italic text-slate-600">{cl.contentPreview}</p>
              <div className="mt-3 flex justify-end gap-2">
                <Button variant="ghost" size="sm" onClick={() => setViewTarget(cl)}>
                  <Eye className="h-4 w-4" /> View
                </Button>
                <Button variant="ghost" size="sm" onClick={() => setRefineTarget(cl)}>
                  <Wand2 className="h-4 w-4" /> Refine
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-red-600 hover:text-red-700"
                  onClick={() => setDeleteTarget(cl)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ViewCoverLetterDialog
        applicationId={applicationId}
        coverLetterId={viewTarget?.id ?? null}
        version={viewTarget?.version}
        onOpenChange={(o) => !o && setViewTarget(null)}
      />
      <RefineCoverLetterDialog
        applicationId={applicationId}
        coverLetterId={refineTarget?.id ?? null}
        version={refineTarget?.version}
        onOpenChange={(o) => !o && setRefineTarget(null)}
      />

      <Dialog open={!!deleteTarget} onOpenChange={(o) => !o && setDeleteTarget(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Delete cover letter</DialogTitle>
            <DialogDescription>
              Delete version {deleteTarget?.version}? This cannot be undone.
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
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
