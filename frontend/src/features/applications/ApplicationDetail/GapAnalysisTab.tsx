import { AlertCircle, Check, Loader2, Sparkles, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useGapAnalysis, useRunGapAnalysis } from '../hooks'
import type { GapAnalysis, SkillMatch } from '../types'

function scoreStyle(score: number): { bar: string; text: string } {
  if (score >= 0.75) return { bar: 'bg-green-500', text: 'text-green-700' }
  if (score >= 0.5) return { bar: 'bg-amber-500', text: 'text-amber-700' }
  return { bar: 'bg-red-500', text: 'text-red-700' }
}

function pct(v: number): number {
  return Math.round(v * 100)
}

function AnalyzingState() {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <Loader2 className="mb-3 h-7 w-7 animate-spin text-slate-400" />
      <p className="text-sm font-medium text-slate-700">Analyzing job requirements against your CV…</p>
      <p className="mt-1 text-xs text-slate-400">This usually takes about 5–10 seconds.</p>
    </div>
  )
}

function RunCta({ onRun, noCv }: { onRun: () => void; noCv: boolean }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-16 text-center">
      <Sparkles className="mb-3 h-8 w-8 text-slate-400" />
      <h3 className="text-lg font-semibold text-slate-900">Gap analysis</h3>
      <p className="mt-1 max-w-md text-sm text-slate-500">
        Extract the skills this job requires and match them against your master CV to see where you
        stand and what to learn.
      </p>
      {noCv && (
        <p className="mt-3 flex items-center gap-1 text-xs text-amber-700">
          <AlertCircle className="h-3.5 w-3.5" />
          Upload your CV first — analysis needs it for matching.
        </p>
      )}
      <Button className="mt-4" onClick={onRun}>
        <Sparkles className="h-4 w-4" />
        Run gap analysis
      </Button>
    </div>
  )
}

function SkillChips({ skills }: { skills: SkillMatch[] }) {
  return (
    <div className="flex flex-wrap gap-2">
      {skills.map((s) => (
        <span
          key={s.requiredSkillName}
          className="inline-flex items-center gap-1 rounded-full bg-green-50 px-2.5 py-1 text-xs font-medium text-green-700"
        >
          <Check className="h-3 w-3" />
          {s.requiredSkillName}
          <span className="text-green-500">{pct(s.similarity)}%</span>
        </span>
      ))}
    </div>
  )
}

function Results({ data, onReanalyze, reanalyzing }: { data: GapAnalysis; onReanalyze: () => void; reanalyzing: boolean }) {
  const style = scoreStyle(data.score)
  const critical = data.missingSkills.filter((s) => s.required)
  const niceToHave = data.missingSkills.filter((s) => !s.required)

  return (
    <div className="max-w-3xl space-y-8">
      <section>
        <div className="mb-2 flex items-baseline justify-between">
          <span className={cn('text-3xl font-bold', style.text)}>{pct(data.score)}% match</span>
          <span className="text-sm text-slate-500">
            {data.matchedRequiredSkills} of {data.totalRequiredSkills} required skills
          </span>
        </div>
        <div className="h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
          <div className={cn('h-full rounded-full transition-all', style.bar)} style={{ width: `${pct(data.score)}%` }} />
        </div>
      </section>

      {data.matchedSkills.length > 0 && (
        <section>
          <h3 className="mb-3 text-sm font-semibold text-slate-900">
            Matched skills ({data.matchedSkills.length})
          </h3>
          <SkillChips skills={data.matchedSkills} />
        </section>
      )}

      {critical.length > 0 && (
        <section>
          <h3 className="mb-3 text-sm font-semibold text-slate-900">Critical gaps ({critical.length})</h3>
          <div className="flex flex-wrap gap-2">
            {critical.map((s) => (
              <span
                key={s.requiredSkillName}
                className="inline-flex items-center gap-1 rounded-full bg-red-50 px-2.5 py-1 text-xs font-medium text-red-700"
              >
                <X className="h-3 w-3" />
                {s.requiredSkillName} · Required
              </span>
            ))}
          </div>
        </section>
      )}

      {niceToHave.length > 0 && (
        <section>
          <h3 className="mb-3 text-sm font-semibold text-slate-900">Nice-to-have gaps ({niceToHave.length})</h3>
          <div className="flex flex-wrap gap-2">
            {niceToHave.map((s) => (
              <span
                key={s.requiredSkillName}
                className="inline-flex items-center rounded-full bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-700"
              >
                {s.requiredSkillName} · Nice to have
              </span>
            ))}
          </div>
        </section>
      )}

      <Button variant="outline" size="sm" onClick={onReanalyze} disabled={reanalyzing}>
        {reanalyzing && <Loader2 className="h-4 w-4 animate-spin" />}
        Re-analyze
      </Button>
    </div>
  )
}

export function GapAnalysisTab({ applicationId }: { applicationId: string }) {
  const query = useGapAnalysis(applicationId)
  const run = useRunGapAnalysis(applicationId)

  if (run.isPending) return <AnalyzingState />
  if (query.isLoading) {
    return (
      <div className="py-16 text-center">
        <Loader2 className="mx-auto h-6 w-6 animate-spin text-slate-400" />
      </div>
    )
  }

  const data = query.data
  const errStatus = (query.error as { response?: { status?: number } } | null)?.response?.status
  const notAnalyzed =
    !!query.error || !data || (data.matchedSkills.length === 0 && data.missingSkills.length === 0)

  if (notAnalyzed) {
    return <RunCta onRun={() => run.mutate()} noCv={errStatus === 422} />
  }

  return <Results data={data} onReanalyze={() => run.mutate()} reanalyzing={run.isPending} />
}
