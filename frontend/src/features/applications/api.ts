import { apiClient } from '@/api/client'
import type {
  Application,
  ApplicationStatus,
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
  // Backend ApplicationFilters: statuses (multi-value) + q (search).
  statuses?: ApplicationStatus[]
  q?: string
}

export const applicationsApi = {
  list: (params: ListParams = {}) => {
    // Backend binds List<ApplicationStatus> via a single comma-separated param
    // (?statuses=APPLIED,SCREENING) rather than repeated keys — serialize explicitly
    // so we don't depend on axios's default array-param encoding.
    const { statuses, ...rest } = params
    return apiClient
      .get<Page<Application>>('/applications', {
        params: { ...rest, statuses: statuses?.length ? statuses.join(',') : undefined },
      })
      .then((r) => r.data)
  },

  getById: (id: string) =>
    apiClient.get<Application>(`/applications/${id}`).then((r) => r.data),

  create: (data: CreateApplicationRequest) =>
    apiClient.post<Application>('/applications', data).then((r) => r.data),

  update: (id: string, data: UpdateApplicationRequest) =>
    apiClient.patch<Application>(`/applications/${id}`, data).then((r) => r.data),

  updateStatus: (id: string, data: UpdateStatusRequest) =>
    apiClient.patch<Application>(`/applications/${id}/status`, data).then((r) => r.data),

  delete: (id: string) => apiClient.delete(`/applications/${id}`).then(() => undefined),
}

export const gapAnalysisApi = {
  get: (applicationId: string) =>
    apiClient.get<GapAnalysis>(`/applications/${applicationId}/gap-analysis`).then((r) => r.data),

  run: (applicationId: string) =>
    apiClient.post<GapAnalysis>(`/applications/${applicationId}/gap-analysis`).then((r) => r.data),
}
