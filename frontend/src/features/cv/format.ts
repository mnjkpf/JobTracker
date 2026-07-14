const MONTH_YEAR = new Intl.DateTimeFormat('en-US', { month: 'short', year: 'numeric' })

export function formatMonthYear(iso: string | null): string {
  if (!iso) return ''
  return MONTH_YEAR.format(new Date(iso))
}

export function formatDateRange(start: string | null, end: string | null): string {
  const startText = formatMonthYear(start)
  const endText = end ? formatMonthYear(end) : 'Present'
  if (!startText) return endText
  return `${startText} – ${endText}`
}
