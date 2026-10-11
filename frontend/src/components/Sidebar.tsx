import { NavLink, useNavigate } from 'react-router-dom'
import { Briefcase, User, BarChart3, LogIn, LogOut, Sparkles } from 'lucide-react'
import { cn } from '@/lib/utils'
import { getUserEmailFromToken } from '@/lib/jwt'
import { useAccount } from '@/features/account/hooks'

const NAV_ITEMS = [
  { to: '/', label: 'Applications', icon: Briefcase, end: true },
  { to: '/cv', label: 'My CV', icon: User, end: false },
  { to: '/statistics', label: 'Statistics', icon: BarChart3, end: false },
]

export function Sidebar() {
  const navigate = useNavigate()
  const email = getUserEmailFromToken()
  const { data: account } = useAccount()
  const isGuest = account?.guest ?? false
  const generations = account?.aiGenerationsRemaining ?? null

  const handleLogout = () => {
    localStorage.clear()
    navigate('/login')
  }

  return (
    <aside className="flex h-screen w-[280px] shrink-0 flex-col border-r border-slate-200 bg-slate-50 p-6">
      <div className="flex items-center gap-2 px-2">
        <Briefcase className="h-5 w-5 text-slate-900" />
        <span className="text-lg font-semibold text-slate-900">JobTracker</span>
      </div>

      <nav className="mt-8 flex flex-col gap-1">
        {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-100',
                isActive && 'bg-slate-200 font-medium text-slate-900 hover:bg-slate-200',
              )
            }
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="mt-auto border-t border-slate-200 pt-4">
        {isGuest ? (
          <>
            <p className="px-3 text-xs font-medium text-slate-700">Guest session</p>
            <p className="px-3 text-xs text-slate-500">
              {generations !== null
                ? `${generations} AI generation${generations === 1 ? '' : 's'} left`
                : 'Not saved permanently'}
            </p>
            <button
              type="button"
              onClick={() => navigate('/register')}
              className="mt-2 flex w-full items-center gap-3 rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-slate-800"
            >
              <Sparkles className="h-4 w-4" />
              Save my board
            </button>
            {/* A guest may already own an account — give them a way in without
                having to clear storage by hand. */}
            <button
              type="button"
              onClick={handleLogout}
              className="mt-1 flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-100"
            >
              <LogIn className="h-4 w-4" />
              Sign in instead
            </button>
          </>
        ) : (
          <>
            {email && <p className="truncate px-3 text-xs text-slate-500" title={email}>{email}</p>}
            <button
              type="button"
              onClick={handleLogout}
              className="mt-2 flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-100"
            >
              <LogOut className="h-4 w-4" />
              Logout
            </button>
          </>
        )}
      </div>
    </aside>
  )
}
