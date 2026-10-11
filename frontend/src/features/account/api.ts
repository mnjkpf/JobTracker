import { apiClient } from '@/api/client'
import type { AuthResponse, RegisterRequest } from '@/features/auth/api'

export interface Account {
  id: string
  /** null for a guest session. */
  email: string | null
  displayName: string | null
  guest: boolean
  /** When the guest session is wiped. null for registered users. */
  guestExpiresAt: string | null
  /** AI generations left for a guest. null = unlimited (registered user). */
  aiGenerationsRemaining: number | null
}

export const accountApi = {
  /** Creates an anonymous session so the tracker is usable without signing up. */
  createGuest: () =>
    apiClient.post<AuthResponse>('/auth/guest').then((r) => r.data),

  get: () => apiClient.get<Account>('/account/me').then((r) => r.data),

  /**
   * Turns the current guest session into a real account. Same user row on the
   * backend, so every application and CV stays — nothing is migrated or lost.
   */
  upgrade: (data: RegisterRequest) =>
    apiClient.post<AuthResponse>('/account/upgrade', data).then((r) => r.data),
}
