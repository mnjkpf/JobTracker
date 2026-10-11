import { useNavigate } from 'react-router-dom'
import { Sparkles, TriangleAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useAccount } from './hooks'

/** Days until the guest session is wiped; 0 means "today". */
function daysLeft(expiresAt: string): number {
  const ms = new Date(expiresAt).getTime() - Date.now()
  return Math.max(0, Math.ceil(ms / (24 * 60 * 60 * 1000)))
}

/**
 * Shown only for guest sessions. Two jobs: make it unmistakable that the data
 * is temporary, and give a one-click path to keeping it.
 */
export function GuestBanner() {
  const navigate = useNavigate()
  const { data: account } = useAccount()

  if (!account?.guest) return null

  const days = account.guestExpiresAt ? daysLeft(account.guestExpiresAt) : null
  const generations = account.aiGenerationsRemaining
  const outOfGenerations = generations !== null && generations <= 0

  return (
    <div className="mb-5 flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3">
      <TriangleAlert className="h-5 w-5 shrink-0 text-amber-600" />

      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-amber-900">
          You&apos;re using JobTracker as a guest
          {days !== null && (
            <> — this board disappears in {days === 0 ? 'less than a day' : `${days} day${days === 1 ? '' : 's'}`}</>
          )}
        </p>
        <p className="mt-0.5 text-xs text-amber-800">
          {outOfGenerations ? (
            <>You&apos;ve used all free AI generations. Create an account to keep generating — everything already on your board stays.</>
          ) : (
            <>
              Create a free account to keep everything permanently
              {generations !== null && <> · {generations} AI generation{generations === 1 ? '' : 's'} left</>}
            </>
          )}
        </p>
      </div>

      <Button size="sm" className="shrink-0" onClick={() => navigate('/register')}>
        <Sparkles className="h-4 w-4" />
        Save my board
      </Button>
    </div>
  )
}
