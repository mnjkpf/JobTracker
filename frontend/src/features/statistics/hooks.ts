import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { applicationsApi } from '@/features/applications/api'
import { useStatuses } from '@/features/statuses/hooks'
import type { Application, SystemStatusType } from '@/features/applications/types'
import type { Status } from '@/features/statuses/api'

// System-type sets drive the headline metrics. Custom statuses (systemType null)
// still appear in the breakdown chart but don't count toward these.
const ACTIVE = new Set<SystemStatusType>(['APPLIED', 'SCREENING', 'INTERVIEW', 'FINAL'])
const RESPONDED = new Set<SystemStatusType>([
  'APPLIED',
  'SCREENING',
  'INTERVIEW',
  'FINAL',
  'OFFER',
  'REJECTED',
])

const MONTH_LABEL = new Intl.DateTimeFormat('en-US', { month: 'short', year: '2-digit' })

export interface StatusSlice {
  id: string
  name: string
  color: string
  count: number
}

export interface Statistics {
  total: number
  currentlyActive: number
  responseRate: number
  offers: number
  byStatus: StatusSlice[]
  perMonth: { label: string; count: number }[]
}

function inSet(set: Set<SystemStatusType>, t: SystemStatusType | null): boolean {
  return t != null && set.has(t)
}

function computeStatistics(applications: Application[], statuses: Status[]): Statistics {
  const total = applications.length

  const byStatus: StatusSlice[] = statuses
    .slice()
    .sort((a, b) => a.position - b.position)
    .map((s) => ({
      id: s.id,
      name: s.name,
      color: s.color,
      count: applications.filter((a) => a.status.id === s.id).length,
    }))

  const currentlyActive = applications.filter((a) => inSet(ACTIVE, a.status.systemType)).length
  const responded = applications.filter((a) => inSet(RESPONDED, a.status.systemType)).length
  const responseRate = total > 0 ? Math.round((responded / total) * 100) : 0
  const offers = applications.filter((a) => a.status.systemType === 'OFFER').length

  // Last 6 months (oldest -> newest), bucketed by createdAt.
  const now = new Date()
  const months: { key: string; label: string }[] = []
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push({ key: `${d.getFullYear()}-${d.getMonth()}`, label: MONTH_LABEL.format(d) })
  }
  const perMonth = months.map(({ key, label }) => ({
    label,
    count: applications.filter((a) => {
      const d = new Date(a.createdAt)
      return `${d.getFullYear()}-${d.getMonth()}` === key
    }).length,
  }))

  return { total, currentlyActive, responseRate, offers, byStatus, perMonth }
}

export const useStatistics = () => {
  const appsQuery = useQuery({
    queryKey: ['applications', { all: true }],
    queryFn: () => applicationsApi.list({ page: 0, size: 500, sort: 'createdAt,desc' }),
  })
  const statusesQuery = useStatuses()

  const statistics = useMemo(
    () =>
      appsQuery.data && statusesQuery.data
        ? computeStatistics(appsQuery.data.content, statusesQuery.data)
        : null,
    [appsQuery.data, statusesQuery.data],
  )

  return {
    statistics,
    isLoading: appsQuery.isLoading || statusesQuery.isLoading,
    isError: appsQuery.isError || statusesQuery.isError,
    refetch: () => {
      appsQuery.refetch()
      statusesQuery.refetch()
    },
  }
}
