import { useMutation } from '@tanstack/react-query'
import { useNavigate, type NavigateFunction } from 'react-router-dom'
import { toast } from 'sonner'
import { authApi } from './api'
import type { AuthResponse, LoginRequest, RegisterRequest } from './api'

function persistSession(response: AuthResponse, navigate: NavigateFunction) {
  localStorage.setItem('accessToken', response.accessToken)
  localStorage.setItem('refreshToken', response.refreshToken)
  navigate('/')
}

// Distinguishes "backend unreachable" from an actual API error — a static
// message here previously always blamed "email already in use", even when
// the real cause was the backend being down (net::ERR_CONNECTION_REFUSED).
function errorMessage(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { detail?: string } } }
  if (!e?.response) return 'Cannot reach the server. Is the backend running?'
  return e.response.data?.detail ?? fallback
}

export const useLogin = () => {
  const navigate = useNavigate()
  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (response) => persistSession(response, navigate),
    onError: (error) => toast.error(errorMessage(error, 'Login failed — check your email and password.')),
  })
}

export const useRegister = () => {
  const navigate = useNavigate()
  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
    onSuccess: (response) => persistSession(response, navigate),
    onError: (error) => toast.error(errorMessage(error, 'Registration failed. Please try again.')),
  })
}
