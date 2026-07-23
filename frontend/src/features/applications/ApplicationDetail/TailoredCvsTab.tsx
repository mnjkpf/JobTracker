import { useState } from 'react'
import { toast } from 'sonner'
import { Download, Eye, Loader2, Sparkles, Trash2 } from 'lucide-react'
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
import {
  useAtsScore,
  useDeleteTailoredCv,
  useDownloadDocx,
  useGenerateTailoredCv,
  useTailoredCvs,
} from '@/features/tailoredCvs/hooks'
import type { TailoredCvSummary } from '@/features/tailoredCvs/api'
import { ViewTailoredCvDialog } from './ViewTailoredCvDialog'

function scoreStyle(score: number): { bar: string; text: string } {
  if (score >= 75) return { bar: 'bg-green-500', text: 'text-green-700' }
  if (score >= 50) return { bar: 'bg-amber-500', text: 'text-amber-700' }
  return { bar: 'bg-red-500', text: 'text-red-700' }
}

function fmt(iso: string) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

function TailoredCvCard({
  applicationId,
  cv,
  onView,
  onDelete,
}: {
  applicationId: string
  cv: TailoredCvSummary
  onView: (cv: TailoredCvSummary) => void
  onDelete: (cv: TailoredCvSummary) => void
}) {
  const ats = useAtsScore(applicationId, cv.id)
  const download = useDownloadDocx(applicationId)

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-shadow hover:shadow-md">
      <div className="flex items-center justify-between gap-3">
        <div className="text-sm font-medium text-slate-900">
          Version {cv.version} · <span className="text-slate-500">{cv.language}</span>
        </div>
        <span className="text-xs text-slate-400">{fmt(cv.createdAt)}</span>
      </div>
      {cv.headline && <p className="mt-0.5 text-xs text-slate-500">{cv.headline}</p>}

      <div className="mt-3">
        {ats.isLoading ? (
          <span className="text-xs text-slate-400">Loading ATS score…</span>
        ) : ats.isError || !ats.data ? (
          <span className="text-xs text-slate-400">ATS score unavailable</span>
        ) : (
          <>
            <div className="mb-1 flex items-center justify-between text-xs">
              <span className={cn('font-semibold', scoreStyle(ats.data.score).text)}>
                ATS {ats.data.score}%
              </span>
              <span className="text-slate-400">
                {ats.data.matchedCount}/{ats.data.totalKeywords} keywords
              </span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
              <div
                className={cn('h-full rounded-full transition-all', scoreStyle(ats.data.score).bar)}
                style={{ width: `${ats.data.score}%` }}
              />
            </div>
          </>
        )}
      </div>

      <div className="mt-3 flex justify-end gap-2">
        <Button variant="ghost" size="sm" onClick={() => onView(cv)}>
          <Eye className="h-4 w-4" /> View
        </Button>
        <Button
          variant="ghost"
          size="sm"
          disabled={download.isPending}
          onClick={() => {
            toast('Downloading DOCX…')
            download.mutate({ id: cv.id, version: cv.version })
          }}
        >
          {download.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
          DOCX
        </Button>
        <Button
          variant="ghost"
          size="sm"
          className="text-red-600 hover:text-red-700"
          onClick={() => onDelete(cv)}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}

export function TailoredCvsTab({ applicationId }: { applicationId: string }) {
  const { data, isLoading } = useTailoredCvs(applicationId)
  const generate = useGenerateTailoredCv(applicationId)
  const del = useDeleteTailoredCv(applicationId)

  const [viewTarget, setViewTarget] = useState<TailoredCvSummary | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<TailoredCvSummary | null>(null)

  const cvs = data ?? []

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">
          {generate.isPending
            ? 'Generating tailored CV… about 10–15 seconds'
            : `${cvs.length} version${cvs.length === 1 ? '' : 's'}`}
        </p>
        <Button onClick={() => generate.mutate({})} disabled={generate.isPending}>
          {generate.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
          Generate tailored CV
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1].map((i) => (
            <div key={i} className="h-32 animate-pulse rounded-lg bg-slate-100" />
          ))}
        </div>
      ) : cvs.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-16 text-center">
          <Sparkles className="mb-3 h-8 w-8 text-slate-400" />
          <h3 className="text-lg font-semibold text-slate-900">No tailored CVs yet</h3>
          <p className="mt-1 max-w-sm text-sm text-slate-500">
            Generate one optimized for this job and check its ATS keyword score.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {cvs.map((cv) => (
            <TailoredCvCard
              key={cv.id}
              applicationId={applicationId}
              cv={cv}
              onView={setViewTarget}
              onDelete={setDeleteTarget}
            />
          ))}
        </div>
      )}

      <ViewTailoredCvDialog
        applicationId={applicationId}
        tailoredCvId={viewTarget?.id ?? null}
        version={viewTarget?.version}
        onOpenChange={(o) => !o && setViewTarget(null)}
      />

      <Dialog open={!!deleteTarget} onOpenChange={(o) => !o && setDeleteTarget(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Delete tailored CV</DialogTitle>
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
