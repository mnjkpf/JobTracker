import { Button } from '@/components/ui/button'
import { useStatistics } from '@/features/statistics/hooks'
import { MetricCard } from '@/features/statistics/MetricCard'
import { StatusBreakdownChart } from '@/features/statistics/StatusBreakdownChart'
import { ApplicationsPerMonthChart } from '@/features/statistics/ApplicationsPerMonthChart'

function PageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-24 animate-pulse rounded-lg bg-slate-100" />
        ))}
      </div>
      <div className="h-64 animate-pulse rounded-lg bg-slate-100" />
      <div className="h-64 animate-pulse rounded-lg bg-slate-100" />
    </div>
  )
}

function ErrorBanner({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-red-200 bg-red-50 px-4 py-3">
      <p className="text-sm text-red-700">Couldn&apos;t load statistics. Is the backend running?</p>
      <Button variant="outline" size="sm" onClick={onRetry}>
        Retry
      </Button>
    </div>
  )
}

export default function StatisticsPage() {
  const { statistics, isLoading, isError, refetch } = useStatistics()

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-6">
        <h2 className="text-2xl font-bold tracking-tight text-slate-900">Statistics</h2>
        <p className="text-sm text-slate-500">How your job search is going.</p>
      </div>

      {isLoading ? (
        <PageSkeleton />
      ) : isError || !statistics ? (
        <ErrorBanner onRetry={() => refetch()} />
      ) : statistics.total === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-20 text-center">
          <h3 className="text-lg font-semibold text-slate-900">No applications yet</h3>
          <p className="mt-1 max-w-sm text-sm text-slate-500">
            Add applications to your board to see stats here.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-4 gap-4">
            <MetricCard value={statistics.total} label="Total applications" />
            <MetricCard value={`${statistics.responseRate}%`} label="Response rate" subtitle="Got any reply" />
            <MetricCard value={statistics.currentlyActive} label="Currently active" subtitle="In progress" />
            <MetricCard value={statistics.offers} label="Offers" />
          </div>

          <StatusBreakdownChart byStatus={statistics.byStatus} />
          <ApplicationsPerMonthChart perMonth={statistics.perMonth} />

          {/* Top Technologies skipped: ApplicationSummaryResponse doesn't expose
              ApplicationSkill, and it's only populated after gap-analysis has run
              per application — no bulk endpoint exists to aggregate it cheaply. */}
        </div>
      )}
    </div>
  )
}
