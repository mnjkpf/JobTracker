import type { ReactNode } from 'react'
import { Loader2 } from 'lucide-react'
import { useEnsureSession } from '@/features/account/hooks'

/**
 * Gate for the app shell. A first-time visitor has no token, so instead of
 * bouncing them to /login we silently open a guest session — the tracker is
 * usable immediately, and signing up later keeps everything they added.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const session = useEnsureSession()

  if (session === 'checking') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
      </div>
    )
  }

  if (session === 'failed') {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50 px-6 text-center">
        <h1 className="text-lg font-semibold text-slate-900">Cannot reach the server</h1>
        <p className="max-w-sm text-sm text-slate-500">
          JobTracker could not start a session. Check your connection and reload the page.
        </p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="mt-1 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          Reload
        </button>
      </div>
    )
  }

  return <>{children}</>
}
