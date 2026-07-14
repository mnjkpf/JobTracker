import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { STATUS_META } from '@/features/applications/statusMeta'
import { STATUS_HEX } from './statusColors'
import type { Statistics } from './hooks'

interface Props {
  byStatus: Statistics['byStatus']
}

export function StatusBreakdownChart({ byStatus }: Props) {
  const data = byStatus
    .filter((s) => s.count > 0)
    .map((s) => ({ status: s.status, label: STATUS_META[s.status].label, count: s.count }))

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Status breakdown</CardTitle>
      </CardHeader>
      <CardContent>
        {data.length === 0 ? (
          <p className="py-8 text-center text-sm text-slate-500">No applications yet.</p>
        ) : (
          <ResponsiveContainer width="100%" height={Math.max(160, data.length * 40)}>
            <BarChart data={data} layout="vertical" margin={{ left: 8, right: 24 }}>
              <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
              <YAxis
                type="category"
                dataKey="label"
                width={80}
                tick={{ fontSize: 12, fill: '#334155' }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                cursor={{ fill: '#f1f5f9' }}
                contentStyle={{ fontSize: 12, borderRadius: 6, borderColor: '#e2e8f0' }}
              />
              <Bar dataKey="count" radius={[0, 4, 4, 0]} barSize={20}>
                {data.map((entry) => (
                  <Cell key={entry.status} fill={STATUS_HEX[entry.status]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  )
}
