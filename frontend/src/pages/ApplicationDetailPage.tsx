import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Loader2, Pencil, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { cn } from '@/lib/utils'
import { useApplication, useDeleteApplication } from '@/features/applications/hooks'
import { STATUS_META } from '@/features/applications/statusMeta'
import { OverviewTab } from '@/features/applications/ApplicationDetail/OverviewTab'
import { GapAnalysisTab } from '@/features/applications/ApplicationDetail/GapAnalysisTab'
import { CoverLettersTab } from '@/features/applications/ApplicationDetail/CoverLettersTab'
import { TailoredCvsTab } from '@/features/applications/ApplicationDetail/TailoredCvsTab'
import { InterviewPrepTab } from '@/features/applications/ApplicationDetail/InterviewPrepTab'
import { EditApplicationDialog } from '@/features/applications/ApplicationDetail/EditApplicationDialog'

function HeaderSkeleton() {
  return (
    <div className="space-y-3">
      <div className="h-8 w-72 animate-pulse rounded bg-slate-200" />
      <div className="h-4 w-48 animate-pulse rounded bg-slate-200" />
      <div className="h-10 w-full animate-pulse rounded bg-slate-100" />
    </div>
  )
}

function NotFound({ onBack }: { onBack: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-20 text-center">
      <h2 className="text-lg font-semibold text-slate-900">Application not found</h2>
      <p className="mt-1 text-sm text-slate-500">It may have been deleted.</p>
      <Button className="mt-4" variant="outline" onClick={onBack}>
        Back to board
      </Button>
    </div>
  )
}

export default function ApplicationDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { data: app, isLoading, isError } = useApplication(id)
  const del = useDeleteApplication()
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)

  const handleDelete = () => {
    del.mutate(id, { onSuccess: () => navigate('/') })
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <div className="mx-auto max-w-5xl px-6 py-6">
        <button
          onClick={() => navigate('/')}
          className="mb-4 inline-flex items-center gap-1 text-sm text-slate-500 transition-colors hover:text-slate-900"
        >
          <ArrowLeft className="h-4 w-4" /> Back to board
        </button>

        {isLoading ? (
          <HeaderSkeleton />
        ) : isError || !app ? (
          <NotFound onBack={() => navigate('/')} />
        ) : (
          <>
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-3">
                  <h1 className="text-2xl font-bold tracking-tight text-slate-900">{app.name}</h1>
                  <span
                    className={cn(
                      'rounded-full px-2.5 py-0.5 text-xs font-medium',
                      STATUS_META[app.status].badge,
                    )}
                  >
                    {STATUS_META[app.status].label}
                  </span>
                </div>
                <p className="mt-1 text-sm text-slate-500">
                  {[app.companyName, app.location].filter(Boolean).join(' · ') || '—'}
                </p>
              </div>
              <div className="flex shrink-0 gap-2">
                <Button variant="outline" size="sm" onClick={() => setEditOpen(true)}>
                  <Pencil className="h-4 w-4" /> Edit
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="text-red-600 hover:text-red-700"
                  onClick={() => setDeleteOpen(true)}
                >
                  <Trash2 className="h-4 w-4" /> Delete
                </Button>
              </div>
            </div>

            <div className="mt-6">
              <Tabs defaultValue="overview">
                <TabsList>
                  <TabsTrigger value="overview">Overview</TabsTrigger>
                  <TabsTrigger value="gap">Gap Analysis</TabsTrigger>
                  <TabsTrigger value="cover-letter">Cover Letters</TabsTrigger>
                  <TabsTrigger value="tailored-cv">Tailored CVs</TabsTrigger>
                  <TabsTrigger value="interview-prep">Interview Prep</TabsTrigger>
                </TabsList>
                <TabsContent value="overview">
                  <OverviewTab application={app} />
                </TabsContent>
                <TabsContent value="gap">
                  <GapAnalysisTab applicationId={app.id} />
                </TabsContent>
                <TabsContent value="cover-letter">
                  <CoverLettersTab applicationId={app.id} />
                </TabsContent>
                <TabsContent value="tailored-cv">
                  <TailoredCvsTab applicationId={app.id} />
                </TabsContent>
                <TabsContent value="interview-prep">
                  <InterviewPrepTab applicationId={app.id} status={app.status} />
                </TabsContent>
              </Tabs>
            </div>

            <EditApplicationDialog application={app} open={editOpen} onOpenChange={setEditOpen} />

            <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
              <DialogContent className="max-w-md">
                <DialogHeader>
                  <DialogTitle>Delete application</DialogTitle>
                  <DialogDescription>
                    This permanently deletes {app.name} and its notes, cover letters, tailored CVs and
                    interview prep. This cannot be undone.
                  </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setDeleteOpen(false)}>
                    Cancel
                  </Button>
                  <Button variant="destructive" onClick={handleDelete} disabled={del.isPending}>
                    {del.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                    Delete
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </>
        )}
      </div>
    </div>
  )
}
