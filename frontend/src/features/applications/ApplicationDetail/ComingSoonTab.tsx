import { Sparkles } from 'lucide-react'

export function ComingSoonTab({ feature }: { feature: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-slate-500">
      <Sparkles className="mb-2 h-8 w-8 opacity-40" />
      <p className="text-sm">{feature} coming in the next session</p>
    </div>
  )
}
