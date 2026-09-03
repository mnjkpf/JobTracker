import type { ReactNode } from 'react'
import { ExternalLink } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useStatuses } from '@/features/statuses/hooks'
import { useUpdateStatus } from '../hooks'
import { statusBadgeStyle } from '../statusMeta'
import type { Application } from '../types'
import { ApplicationCvPanel } from './ApplicationCvPanel'

function fmtDate(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

function salaryText(app: Application): string {
  if (app.salaryMin == null && app.salaryMax == null) return '—'
  const cur = app.salaryCurrency ?? ''
  if (app.salaryMin != null && app.salaryMax != null) {
    return `${app.salaryMin.toLocaleString()}–${app.salaryMax.toLocaleString()} ${cur}`.trim()
  }
  const single = (app.salaryMin ?? app.salaryMax) as number
  return `${single.toLocaleString()} ${cur}`.trim()
}

function MetaRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</dt>
      <dd className="mt-0.5 text-sm text-slate-800">{children}</dd>
    </div>
  )
}

export function OverviewTab({ application }: { application: Application }) {
  const updateStatus = useUpdateStatus()
  const { data: statuses } = useStatuses()
  const others = (statuses ?? []).filter((s) => s.id !== application.status.id)

  return (
    <div className="max-w-3xl space-y-8">
      <dl className="grid grid-cols-2 gap-x-8 gap-y-4">
        <MetaRow label="Company">{application.companyName ?? '—'}</MetaRow>
        <MetaRow label="Location">{application.location ?? '—'}</MetaRow>
        <MetaRow label="Seniority">{application.seniority}</MetaRow>
        <MetaRow label="Work mode">{application.workMode}</MetaRow>
        <MetaRow label="Contract">{application.contractType}</MetaRow>
        <MetaRow label="Salary">{salaryText(application)}</MetaRow>
        <MetaRow label="Applied">{fmtDate(application.appliedAt)}</MetaRow>
        <MetaRow label="Last updated">{fmtDate(application.updatedAt)}</MetaRow>
        <MetaRow label="Job posting">
          <a
            href={application.url}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1 text-blue-600 hover:underline"
          >
            Open <ExternalLink className="h-3.5 w-3.5" />
          </a>
        </MetaRow>
      </dl>

      <ApplicationCvPanel applicationId={application.id} />

      {application.description && (
        <section>
          <h3 className="mb-2 text-sm font-semibold text-slate-900">Description</h3>
          <div className="whitespace-pre-wrap rounded-md border border-slate-200 bg-slate-50 p-4 font-mono text-xs leading-relaxed text-slate-700">
            {application.description}
          </div>
        </section>
      )}

      <section>
        <h3 className="mb-3 text-sm font-semibold text-slate-900">Change status</h3>
        <div className="mb-3 flex items-center gap-2 text-sm text-slate-500">
          <span>Current:</span>
          <span
            className="rounded-full px-2 py-0.5 text-xs font-medium"
            style={statusBadgeStyle(application.status.color)}
          >
            {application.status.name}
          </span>
        </div>
        {others.length === 0 ? (
          <p className="text-sm text-slate-400">
            No other statuses yet — add some from &ldquo;Statuses&rdquo; on the board.
          </p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {others.map((s) => (
              <Button
                key={s.id}
                variant="outline"
                size="sm"
                disabled={updateStatus.isPending}
                onClick={() => updateStatus.mutate({ id: application.id, data: { statusId: s.id } })}
              >
                Move to {s.name}
              </Button>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
