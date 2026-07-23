import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { applicationsApi } from '@/features/applications/api'
import type { Application, ApplicationStatus } from '@/features/applications/types'

const ACTIVE_STATUSES: ApplicationStatus[] = ['APPLIED', 'SCREENING', 'INTERVIEW', 'FINAL']
const RESPONSE_STATUSES: ApplicationStatus[] = [...ACTIVE_STATUSES, 'OFFER', 'REJECTED']
const ALL_STATUSES: ApplicationStatus[] = [
  'SAVED',
  'APPLIED',
  'SCREENING',
  'INTERVIEW',
  'FINAL',
  'OFFER',
  'REJECTED',
  'WITHDRAWN',
  'GHOSTED',
]

const MONTH_LABEL = new Intl.DateTimeFormat('en-US', { month: 'short', year: '2-digit' })

export interface Statistics {
  total: number
  currentlyActive: number
  responseRate: number
  offers: number
  byStatus: { status: ApplicationStatus; count: number }[]
  perMonth: { label: string; count: number }[]
}

function computeStatistics(applications: Application[]): Statistics {
  const total = applications.length

  const byStatus = ALL_STATUSES.map((status) => ({
    status,
    count: applications.filter((a) => a.status === status).length,
  }))

  const currentlyActive = applications.filter((a) => ACTIVE_STATUSES.includes(a.status)).length
  const responded = applications.filter((a) => RESPONSE_STATUSES.includes(a.status)).length
  const responseRate = total > 0 ? Math.round((responded / total) * 100) : 0
  const offers = applications.filter((a) => a.status === 'OFFER').length

  // Last 6 months (oldest -> newest), bucketed by createdAt.
  const now = new Date()
  const months: { key: string; label: string; date: Date }[] = []
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push({ key: `${d.getFullYear()}-${d.getMonth()}`, label: MONTH_LABEL.format(d), date: d })
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
  const query = useQuery({
    queryKey: ['applications', { all: true }],
    queryFn: () => applicationsApi.list({ page: 0, size: 500, sort: 'createdAt,desc' }),
  })

  const statistics = useMemo(
    () => (query.data ? computeStatistics(query.data.content) : null),
    [query.data],
  )

  return { ...query, statistics }
}
