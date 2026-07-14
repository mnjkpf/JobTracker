import type { ApplicationStatus } from '@/features/applications/types'

// Hex mirrors of the Tailwind dot colors in statusMeta.ts — recharts needs
// real color values, not utility classes.
export const STATUS_HEX: Record<ApplicationStatus, string> = {
  SAVED: '#94a3b8',
  APPLIED: '#3b82f6',
  SCREENING: '#6366f1',
  INTERVIEW: '#a855f7',
  FINAL: '#f59e0b',
  OFFER: '#22c55e',
  REJECTED: '#ef4444',
  WITHDRAWN: '#9ca3af',
  GHOSTED: '#9ca3af',
}
