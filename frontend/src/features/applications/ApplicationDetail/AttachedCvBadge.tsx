import { FileCheck2 } from 'lucide-react'
import { useApplicationCv } from '../applicationCv/hooks'

/**
 * Small inline indicator shown on AI tabs (cover letter / tailored CV / interview
 * prep) when a CV is attached to this application — signalling that generation
 * uses the attached CV as context instead of the Master CV.
 */
export function AttachedCvBadge({ applicationId }: { applicationId: string }) {
  const { data: cv } = useApplicationCv(applicationId)
  if (!cv) return null
  return (
    <span
      className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700"
      title={`Generation uses the CV attached to this application: ${cv.fileName}`}
    >
      <FileCheck2 className="h-3.5 w-3.5" />
      Using this application&apos;s CV
    </span>
  )
}
