import { NavLink, useNavigate } from 'react-router-dom'
import { Briefcase, User, BarChart3, LogOut } from 'lucide-react'
import { cn } from '@/lib/utils'
import { getUserEmailFromToken } from '@/lib/jwt'

const NAV_ITEMS = [
  { to: '/', label: 'Applications', icon: Briefcase, end: true },
  { to: '/cv', label: 'My CV', icon: User, end: false },
  { to: '/statistics', label: 'Statistics', icon: BarChart3, end: false },
]

export function Sidebar() {
  const navigate = useNavigate()
  const email = getUserEmailFromToken()

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
        {email && <p className="truncate px-3 text-xs text-slate-500" title={email}>{email}</p>}
        <button
          type="button"
          onClick={handleLogout}
          className="mt-2 flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-100"
        >
          <LogOut className="h-4 w-4" />
          Logout
        </button>
      </div>
    </aside>
  )
}
