import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import type { RegisterRequest } from '@/features/auth/api'
import { accountApi } from './api'

const ACCOUNT_KEY = ['account'] as const

export type SessionState = 'checking' | 'ready' | 'failed'

/**
 * Guarantees the app always has a session: if there is no token yet, silently
 * creates a guest one so a first-time visitor lands straight on the board
 * instead of a login wall.
 */
export function useEnsureSession(): SessionState {
  const [state, setState] = useState<SessionState>(() =>
    localStorage.getItem('accessToken') ? 'ready' : 'checking',
  )
  // StrictMode double-invokes effects in dev; this guard keeps it to exactly one
  // guest account instead of two throwaway ones.
  //
  // Deliberately no cleanup flag here: a `cancelled` flag plus this ref cancel
  // each other out. The first mount fires the request, its cleanup flips the
  // flag, the second mount skips the request because of the ref — so the only
  // response in flight gets ignored and the UI spins forever. Settling state on
  // an unmounted component is a harmless no-op in React 18+, so the ref alone
  // is the correct guard.
  const started = useRef(false)

  useEffect(() => {
    if (state !== 'checking' || started.current) return
    started.current = true

    accountApi
      .createGuest()
      .then((response) => {
        localStorage.setItem('accessToken', response.accessToken)
        localStorage.setItem('refreshToken', response.refreshToken)
        setState('ready')
      })
      .catch(() => setState('failed'))
  }, [state])

  return state
}

export const useAccount = () =>
  useQuery({
    queryKey: ACCOUNT_KEY,
    queryFn: () => accountApi.get(),
    staleTime: 60 * 1000,
  })

export const useUpgradeAccount = () => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: RegisterRequest) => accountApi.upgrade(data),
    onSuccess: (response) => {
      // New token pair — the JWT subject switches from "guest:<uuid>" to the email.
      localStorage.setItem('accessToken', response.accessToken)
      localStorage.setItem('refreshToken', response.refreshToken)
      queryClient.invalidateQueries({ queryKey: ACCOUNT_KEY })
      toast.success('Account created — your board is saved for good')
      navigate('/')
    },
    onError: (error) => {
      const e = error as { response?: { data?: { detail?: string } } }
      if (!e?.response) {
        toast.error('Cannot reach the server. Is the backend running?')
        return
      }
      toast.error(e.response.data?.detail ?? 'Could not create the account.')
    },
  })
}
