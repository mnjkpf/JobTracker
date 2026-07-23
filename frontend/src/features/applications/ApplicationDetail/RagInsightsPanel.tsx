import { Brain } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { useRelevantNotes } from '@/features/interviewPrep/hooks'
import type { RelevantNote } from '@/features/interviewPrep/api'

function pct(similarity: number): number {
  return Math.round(similarity * 100)
}

function RelevantNoteCard({ note }: { note: RelevantNote }) {
  const navigate = useNavigate()
  const isReflection = note.noteType === 'POST_INTERVIEW'

  return (
    <div className="rounded-lg border border-purple-100 bg-white/70 p-4">
      <div className="flex items-center justify-between gap-2">
        <span
          className={cn(
            'rounded-full px-2 py-0.5 text-xs font-medium',
            isReflection ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700',
          )}
        >
          {isReflection ? 'Post-interview' : 'Prep note'}
        </span>
        <span className="text-xs font-semibold text-purple-700">{pct(note.similarity)}% match</span>
      </div>
      <p className="mt-2 text-sm leading-relaxed text-slate-700">{note.content}</p>
      <button
        type="button"
        className="mt-2 text-xs font-medium text-blue-600 hover:underline"
        onClick={() => navigate(`/applications/${note.applicationId}`)}
      >
        View past interview →
      </button>
    </div>
  )
}

export function RagInsightsPanel({ applicationId }: { applicationId: string }) {
  const { data, isLoading } = useRelevantNotes(applicationId)
  const notes = data ?? []

  return (
    <div className="rounded-lg border border-purple-200 bg-gradient-to-br from-purple-50 to-blue-50 p-6">
      <div className="mb-3 flex items-center gap-2">
        <Brain className="h-5 w-5 text-purple-600" />
        <h3 className="font-semibold text-slate-900">RAG Insights</h3>
      </div>

      {isLoading ? (
        <div className="h-16 animate-pulse rounded-md bg-white/60" />
      ) : notes.length === 0 ? (
        <p className="text-sm text-slate-600">
          No relevant past notes yet. As you interview and take notes, this system finds patterns
          from your past experiences and uses them to prepare you for similar future interviews.
        </p>
      ) : (
        <>
          <p className="mb-3 text-sm text-slate-700">
            This prep guide was informed by {notes.length} relevant note{notes.length === 1 ? '' : 's'}{' '}
            from your past interviews:
          </p>
          <div className="space-y-3">
            {notes.map((n) => (
              <RelevantNoteCard key={n.id} note={n} />
            ))}
          </div>
          <p className="mt-3 text-xs text-slate-500">
            These insights helped tailor the questions above to focus on your specific weak areas.
          </p>
        </>
      )}
    </div>
  )
}
