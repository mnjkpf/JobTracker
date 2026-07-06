import type { ReactNode } from 'react'
import { Download, Link, Loader2, Mail, Phone } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { useDownloadDocx, useTailoredCv } from '@/features/tailoredCvs/hooks'

function fmtMonth(d: string | null): string | null {
  if (!d) return null
  return new Date(d).toLocaleDateString(undefined, { year: 'numeric', month: 'short' })
}

function range(start: string | null, end: string | null): string {
  const s = fmtMonth(start)
  if (!s) return ''
  return `${s} – ${end ? fmtMonth(end) : 'Present'}`
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-5">
      <h3 className="mb-2 border-b border-slate-200 pb-1 text-xs font-semibold uppercase tracking-wide text-slate-400">
        {title}
      </h3>
      {children}
    </section>
  )
}

interface Props {
  applicationId: string
  tailoredCvId: string | null
  version?: number
  onOpenChange: (open: boolean) => void
}

export function ViewTailoredCvDialog({ applicationId, tailoredCvId, version, onOpenChange }: Props) {
  const { data: cv, isLoading } = useTailoredCv(applicationId, tailoredCvId)
  const download = useDownloadDocx(applicationId)

  return (
    <Dialog open={!!tailoredCvId} onOpenChange={(o) => !o && onOpenChange(false)}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Tailored CV{version ? ` · v${version}` : ''}</DialogTitle>
        </DialogHeader>

        {isLoading || !cv ? (
          <div className="py-16 text-center">
            <Loader2 className="mx-auto h-6 w-6 animate-spin text-slate-400" />
          </div>
        ) : (
          <div className="max-h-[70vh] overflow-y-auto pr-1">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="text-xl font-bold text-slate-900">{cv.fullName}</h2>
                <p className="text-sm text-slate-600">{cv.headline}</p>
                <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                  {cv.email && (
                    <span className="inline-flex items-center gap-1">
                      <Mail className="h-3 w-3" /> {cv.email}
                    </span>
                  )}
                  {cv.phone && (
                    <span className="inline-flex items-center gap-1">
                      <Phone className="h-3 w-3" /> {cv.phone}
                    </span>
                  )}
                  {cv.linkedInUrl && (
                    <a href={cv.linkedInUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 hover:text-slate-800">
                      <Link className="h-3 w-3" /> LinkedIn
                    </a>
                  )}
                  {cv.githubUrl && (
                    <a href={cv.githubUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 hover:text-slate-800">
                      <Link className="h-3 w-3" /> GitHub
                    </a>
                  )}
                </div>
              </div>
              <Button
                size="sm"
                variant="outline"
                disabled={download.isPending}
                onClick={() => download.mutate({ id: cv.id, version: cv.version })}
              >
                {download.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
                DOCX
              </Button>
            </div>

            {cv.summary && (
              <Section title="Summary">
                <p className="text-sm leading-relaxed text-slate-700">{cv.summary}</p>
              </Section>
            )}

            {cv.experiences.length > 0 && (
              <Section title="Experience">
                <div className="space-y-3">
                  {cv.experiences.map((e) => (
                    <div key={e.id}>
                      <p className="text-sm font-medium text-slate-900">
                        {e.position} <span className="font-normal text-slate-500">@ {e.company}</span>
                      </p>
                      <p className="text-xs text-slate-400">
                        {range(e.startDate, e.endDate)}
                        {e.location ? ` · ${e.location}` : ''}
                      </p>
                      {e.description && <p className="mt-1 text-sm text-slate-700">{e.description}</p>}
                    </div>
                  ))}
                </div>
              </Section>
            )}

            {cv.skills.length > 0 && (
              <Section title="Skills">
                <div className="flex flex-wrap gap-1.5">
                  {cv.skills.map((s) => (
                    <span key={s.id} className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs text-slate-700">
                      {s.name}
                    </span>
                  ))}
                </div>
              </Section>
            )}

            {cv.projects.length > 0 && (
              <Section title="Projects">
                <div className="space-y-3">
                  {cv.projects.map((p) => (
                    <div key={p.id}>
                      <p className="text-sm font-medium text-slate-900">
                        {p.name}
                        {p.url && (
                          <a href={p.url} target="_blank" rel="noreferrer" className="ml-2 text-xs font-normal text-blue-600 hover:underline">
                            link
                          </a>
                        )}
                      </p>
                      {p.technologies && <p className="text-xs text-slate-400">{p.technologies}</p>}
                      {p.description && <p className="mt-1 text-sm text-slate-700">{p.description}</p>}
                    </div>
                  ))}
                </div>
              </Section>
            )}

            {cv.educations.length > 0 && (
              <Section title="Education">
                <div className="space-y-2">
                  {cv.educations.map((ed) => (
                    <div key={ed.id}>
                      <p className="text-sm font-medium text-slate-900">
                        {[ed.degree, ed.fieldOfStudy].filter(Boolean).join(', ') || ed.institution}
                      </p>
                      <p className="text-xs text-slate-400">
                        {ed.institution}
                        {range(ed.startDate, ed.endDate) ? ` · ${range(ed.startDate, ed.endDate)}` : ''}
                      </p>
                    </div>
                  ))}
                </div>
              </Section>
            )}

            {cv.languages.length > 0 && (
              <Section title="Languages">
                <div className="flex flex-wrap gap-1.5">
                  {cv.languages.map((l) => (
                    <span key={l.id} className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs text-slate-700">
                      {l.name}
                      {l.level ? ` · ${l.level}` : ''}
                    </span>
                  ))}
                </div>
              </Section>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
