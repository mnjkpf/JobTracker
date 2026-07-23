import type { ReactNode } from 'react'
import { ExternalLink } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useUpdateStatus } from '../hooks'
import { STATUS_META } from '../statusMeta'
import { VALID_TRANSITIONS } from '../types'
import type { Application } from '../types'

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
  const next = VALID_TRANSITIONS[application.status]

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
        {next.length === 0 ? (
          <p className="text-sm text-slate-400">
            <span
              className={cn(
                'mr-2 rounded-full px-2 py-0.5 text-xs font-medium',
                STATUS_META[application.status].badge,
              )}
            >
              {STATUS_META[application.status].label}
            </span>
            is a terminal status — no further moves.
          </p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {next.map((s) => (
              <Button
                key={s}
                variant="outline"
                size="sm"
                disabled={updateStatus.isPending}
                onClick={() => updateStatus.mutate({ id: application.id, data: { status: s } })}
              >
                Move to {STATUS_META[s].label}
              </Button>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
