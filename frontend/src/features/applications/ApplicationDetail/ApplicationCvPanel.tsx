import { useRef } from 'react'
import { Download, FileText, Loader2, Trash2, Upload } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  useApplicationCv,
  useDeleteApplicationCv,
  useDownloadApplicationCv,
  useUploadApplicationCv,
} from '../applicationCv/hooks'

const ACCEPT = '.pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document'

function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export function ApplicationCvPanel({ applicationId }: { applicationId: string }) {
  const { data: cv, isLoading } = useApplicationCv(applicationId)
  const upload = useUploadApplicationCv(applicationId)
  const remove = useDeleteApplicationCv(applicationId)
  const download = useDownloadApplicationCv(applicationId)
  const inputRef = useRef<HTMLInputElement>(null)

  const pickFile = () => inputRef.current?.click()
  const onFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) upload.mutate(file)
    e.target.value = '' // allow re-picking the same file
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4">
      <input ref={inputRef} type="file" accept={ACCEPT} className="hidden" onChange={onFile} />

      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">CV for this application</h3>
          <p className="mt-0.5 text-xs text-slate-500">
            Upload the CV you tailored for this job. It&apos;s used as the AI context for cover
            letters, tailored CVs and interview prep here (Master CV is the fallback).
          </p>
        </div>
      </div>

      <div className="mt-3">
        {isLoading ? (
          <div className="h-12 animate-pulse rounded-md bg-slate-100" />
        ) : cv ? (
          <div className="flex items-center gap-3 rounded-md border border-slate-200 bg-slate-50 p-3">
            <FileText className="h-8 w-8 shrink-0 text-slate-400" />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-slate-800">{cv.fileName}</p>
              <p className="text-xs text-slate-500">
                {humanSize(cv.fileSize)} · attached {fmtDate(cv.createdAt)}
              </p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              disabled={download.isPending}
              onClick={() => download.mutate(cv.fileName)}
            >
              {download.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
              Download
            </Button>
            <Button variant="outline" size="sm" disabled={upload.isPending} onClick={pickFile}>
              {upload.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Upload className="h-4 w-4" />}
              Replace
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="text-red-600 hover:text-red-700"
              disabled={remove.isPending}
              onClick={() => remove.mutate()}
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        ) : (
          <button
            type="button"
            onClick={pickFile}
            disabled={upload.isPending}
            className="flex w-full flex-col items-center justify-center rounded-md border border-dashed border-slate-300 bg-slate-50 py-8 text-center transition-colors hover:border-slate-400 hover:bg-slate-100"
          >
            {upload.isPending ? (
              <Loader2 className="mb-2 h-6 w-6 animate-spin text-slate-400" />
            ) : (
              <Upload className="mb-2 h-6 w-6 text-slate-400" />
            )}
            <span className="text-sm font-medium text-slate-700">Upload a CV (PDF or DOCX)</span>
            <span className="mt-0.5 text-xs text-slate-400">Max 5 MB</span>
          </button>
        )}
      </div>
    </section>
  )
}
