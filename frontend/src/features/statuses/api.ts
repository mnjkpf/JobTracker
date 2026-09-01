import { apiClient } from '@/api/client'
import type { SystemStatusType } from '@/features/applications/types'

export interface Status {
  id: string
  name: string
  color: string // hex
  position: number
  systemType: SystemStatusType | null
  terminal: boolean
}

export interface CreateStatusPayload {
  name: string
  color: string
  terminal?: boolean
}

export interface UpdateStatusPayload {
  name?: string
  color?: string
  terminal?: boolean
}

export const statusesApi = {
  list: () => apiClient.get<Status[]>('/statuses').then((r) => r.data),

  create: (data: CreateStatusPayload) =>
    apiClient.post<Status>('/statuses', data).then((r) => r.data),

  update: (id: string, data: UpdateStatusPayload) =>
    apiClient.patch<Status>(`/statuses/${id}`, data).then((r) => r.data),

  // Backend blocks deleting a status that still has applications unless a
  // reassignTo target is supplied (it moves them there first).
  remove: (id: string, reassignTo?: string) =>
    apiClient
      .delete(`/statuses/${id}`, { params: reassignTo ? { reassignTo } : undefined })
      .then(() => undefined),

  reorder: (orderedIds: string[]) =>
    apiClient.patch<Status[]>('/statuses/reorder', { orderedIds }).then((r) => r.data),
}
