import { Outlet } from 'react-router-dom'
import { Sidebar } from '@/components/Sidebar'

export function AppLayout() {
  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar />
      <main className="min-w-0 flex-1 overflow-x-auto px-6 py-6">
        <Outlet />
      </main>
    </div>
  )
}
