import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import LoginPage from '@/pages/LoginPage'
import RegisterPage from '@/pages/RegisterPage'
import DashboardPage from '@/pages/DashboardPage'
import ApplicationDetailPage from '@/pages/ApplicationDetailPage'
import CvEditorPage from '@/pages/CvEditorPage'
import StatisticsPage from '@/pages/StatisticsPage'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { AppLayout } from '@/layouts/AppLayout'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,
      retry: 1,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<DashboardPage />} />
            <Route path="/applications/:id" element={<ApplicationDetailPage />} />
            <Route path="/cv" element={<CvEditorPage />} />
            <Route path="/statistics" element={<StatisticsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
      <Toaster richColors position="top-right" />
    </QueryClientProvider>
  )
}

export default App
