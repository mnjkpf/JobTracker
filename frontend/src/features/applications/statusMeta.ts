import type { ApplicationStatus } from './types'

/** Columns shown on the board, in flow order. WITHDRAWN/GHOSTED are hidden in MVP. */
export const KANBAN_COLUMNS: { status: ApplicationStatus; label: string }[] = [
  { status: 'SAVED', label: 'Saved' },
  { status: 'APPLIED', label: 'Applied' },
  { status: 'SCREENING', label: 'Screening' },
  { status: 'INTERVIEW', label: 'Interview' },
  { status: 'FINAL', label: 'Final' },
  { status: 'OFFER', label: 'Offer' },
  { status: 'REJECTED', label: 'Rejected' },
]

// Literal class strings so Tailwind's content scanner keeps them (no dynamic concat).
export const STATUS_STYLES: Record<ApplicationStatus, { dot: string; badge: string }> = {
  SAVED: { dot: 'bg-slate-400', badge: 'bg-slate-200 text-slate-700' },
  APPLIED: { dot: 'bg-blue-500', badge: 'bg-blue-50 text-blue-700' },
  SCREENING: { dot: 'bg-indigo-500', badge: 'bg-indigo-50 text-indigo-700' },
  INTERVIEW: { dot: 'bg-purple-500', badge: 'bg-purple-50 text-purple-700' },
  FINAL: { dot: 'bg-amber-500', badge: 'bg-amber-50 text-amber-700' },
  OFFER: { dot: 'bg-green-500', badge: 'bg-green-50 text-green-700' },
  REJECTED: { dot: 'bg-red-500', badge: 'bg-red-50 text-red-700' },
  WITHDRAWN: { dot: 'bg-gray-400', badge: 'bg-gray-100 text-gray-600' },
  GHOSTED: { dot: 'bg-gray-400', badge: 'bg-gray-100 text-gray-600' },
}

// Solid badge (bg-*-100 / text-*-700) + label for detail-page status pills.
export const STATUS_META: Record<ApplicationStatus, { label: string; badge: string }> = {
  SAVED: { label: 'Saved', badge: 'bg-slate-100 text-slate-700' },
  APPLIED: { label: 'Applied', badge: 'bg-blue-100 text-blue-700' },
  SCREENING: { label: 'Screening', badge: 'bg-indigo-100 text-indigo-700' },
  INTERVIEW: { label: 'Interview', badge: 'bg-purple-100 text-purple-700' },
  FINAL: { label: 'Final', badge: 'bg-amber-100 text-amber-700' },
  OFFER: { label: 'Offer', badge: 'bg-green-100 text-green-700' },
  REJECTED: { label: 'Rejected', badge: 'bg-red-100 text-red-700' },
  WITHDRAWN: { label: 'Withdrawn', badge: 'bg-gray-100 text-gray-600' },
  GHOSTED: { label: 'Ghosted', badge: 'bg-gray-100 text-gray-600' },
}
