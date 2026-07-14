import { Card, CardContent } from '@/components/ui/card'

interface Props {
  value: string | number
  label: string
  subtitle?: string
}

export function MetricCard({ value, label, subtitle }: Props) {
  return (
    <Card>
      <CardContent className="p-6">
        <p className="text-3xl font-bold tracking-tight text-slate-900">{value}</p>
        <p className="mt-1 text-sm font-medium text-slate-700">{label}</p>
        {subtitle && <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>}
      </CardContent>
    </Card>
  )
}
