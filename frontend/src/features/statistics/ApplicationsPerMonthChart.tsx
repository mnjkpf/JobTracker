import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Statistics } from './hooks'

interface Props {
  perMonth: Statistics['perMonth']
}

export function ApplicationsPerMonthChart({ perMonth }: Props) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Applications per month</CardTitle>
      </CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={perMonth} margin={{ left: -20 }}>
            <CartesianGrid vertical={false} stroke="#f1f5f9" />
            <XAxis dataKey="label" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
            <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
            <Tooltip cursor={{ fill: '#f1f5f9' }} contentStyle={{ fontSize: 12, borderRadius: 6, borderColor: '#e2e8f0' }} />
            <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} barSize={32} />
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  )
}
