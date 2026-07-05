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

export const useLogin = () => {
  const navigate = useNavigate()
  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (response) => persistSession(response, navigate),
    onError: () => toast.error('Login failed — check your email and password.'),
  })
}

export const useRegister = () => {
  const navigate = useNavigate()
  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
    onSuccess: (response) => persistSession(response, navigate),
    onError: () => toast.error('Registration failed — that email may already be in use.'),
  })
}
