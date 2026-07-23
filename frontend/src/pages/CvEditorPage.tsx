import { useRef, useState } from 'react'
import { FileText, Loader2, Upload } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useCv, useUploadCv } from '@/features/cv/hooks'
import { ProfileSection } from '@/features/cv/ProfileSection'
import { EditProfileDialog } from '@/features/cv/EditProfileDialog'
import { ExperiencesSection } from '@/features/cv/ExperiencesSection'
import { EducationsSection } from '@/features/cv/EducationsSection'
import { SkillsSection } from '@/features/cv/SkillsSection'
import { ProjectsSection } from '@/features/cv/ProjectsSection'
import { LanguagesSection } from '@/features/cv/LanguagesSection'

function statusOf(error: unknown): number | undefined {
  return (error as { response?: { status?: number } })?.response?.status
}

function PageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="h-8 w-64 animate-pulse rounded bg-slate-200" />
      <div className="h-24 w-full animate-pulse rounded bg-slate-100" />
      <div className="h-40 w-full animate-pulse rounded bg-slate-100" />
      <div className="h-40 w-full animate-pulse rounded bg-slate-100" />
    </div>
  )
}

function UploadButton({ confirmReplace, onUploaded }: { confirmReplace?: boolean; onUploaded?: () => void }) {
  const inputRef = useRef<HTMLInputElement>(null)
  const upload = useUploadCv()

  const handleFile = (file: File | undefined) => {
    if (!file) return
    if (confirmReplace && !window.confirm('This replaces your entire current CV with the extracted content. Continue?')) {
      return
    }
    upload.mutate(file, { onSuccess: () => onUploaded?.() })
  }

  return (
    <>
      <input
        ref={inputRef}
        type="file"
        accept=".pdf,.doc,.docx"
        className="hidden"
        onChange={(e) => {
          handleFile(e.target.files?.[0])
          e.target.value = ''
        }}
      />
      <Button variant="outline" onClick={() => inputRef.current?.click()} disabled={upload.isPending}>
        {upload.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Upload className="h-4 w-4" />}
        Upload PDF/DOCX
      </Button>
    </>
  )
}

function EmptyState() {
  const [createOpen, setCreateOpen] = useState(false)
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-20 text-center">
      <FileText className="h-10 w-10 text-slate-300" />
      <h3 className="mt-3 text-lg font-semibold text-slate-900">No CV yet</h3>
      <p className="mt-1 max-w-sm text-sm text-slate-500">
        Upload your existing CV to auto-extract, or start building one from scratch.
      </p>
      <div className="mt-4 flex gap-2">
        <UploadButton />
        <Button onClick={() => setCreateOpen(true)}>Create empty CV</Button>
      </div>
      <EditProfileDialog cv={null} open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  )
}

export default function CvEditorPage() {
  const { data: cv, isLoading, isError, error } = useCv()
  const notFound = isError && statusOf(error) === 404

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-900">My CV</h2>
          <p className="text-sm text-slate-500">Your master CV, used to tailor applications.</p>
        </div>
        {cv && <UploadButton confirmReplace />}
      </div>

      {isLoading ? (
        <PageSkeleton />
      ) : notFound || !cv ? (
        <EmptyState />
      ) : (
        <div>
          <ProfileSection cv={cv} />
          <ExperiencesSection experiences={cv.experiences} />
          <SkillsSection skills={cv.skills} />
          <EducationsSection educations={cv.educations} />
          <ProjectsSection projects={cv.projects} />
          <LanguagesSection languages={cv.languages} />
        </div>
      )}
    </div>
  )
}
