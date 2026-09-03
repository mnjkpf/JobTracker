import { apiClient } from '@/api/client'
import type {
  Application,
  CreateApplicationRequest,
  GapAnalysis,
  Page,
  UpdateApplicationRequest,
  UpdateStatusRequest,
} from './types'

export interface ListParams {
  page?: number
  size?: number
  sort?: string
  q?: string
  // archived omitted/false → active board; true → archived list.
  archived?: boolean
}

export const applicationsApi = {
  list: (params: ListParams = {}) =>
    apiClient.get<Page<Application>>('/applications', { params }).then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<Application>(`/applications/${id}`).then((r) => r.data),

  create: (data: CreateApplicationRequest) =>
    apiClient.post<Application>('/applications', data).then((r) => r.data),

  update: (id: string, data: UpdateApplicationRequest) =>
    apiClient.patch<Application>(`/applications/${id}`, data).then((r) => r.data),

  // Status change now targets a status id (custom per-user statuses).
  updateStatus: (id: string, data: UpdateStatusRequest) =>
    apiClient.patch<Application>(`/applications/${id}/status`, data).then((r) => r.data),

  archive: (id: string) =>
    apiClient.post(`/applications/${id}/archive`).then(() => undefined),

  unarchive: (id: string) =>
    apiClient.post(`/applications/${id}/unarchive`).then(() => undefined),

  // Hard delete (backend DELETE is a real delete now, not soft-archive).
  delete: (id: string) => apiClient.delete(`/applications/${id}`).then(() => undefined),
}

export const gapAnalysisApi = {
  get: (applicationId: string) =>
    apiClient.get<GapAnalysis>(`/applications/${applicationId}/gap-analysis`).then((r) => r.data),

  run: (applicationId: string) =>
    apiClient.post<GapAnalysis>(`/applications/${applicationId}/gap-analysis`).then((r) => r.data),
}
