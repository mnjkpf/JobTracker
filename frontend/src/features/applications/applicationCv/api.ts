import { apiClient } from '@/api/client'

export interface ApplicationCvMeta {
  id: string
  fileName: string
  contentType: string
  fileSize: number
  createdAt: string
}

export const applicationCvApi = {
  // 404 → no CV attached yet; treat as null rather than an error.
  get: (appId: string): Promise<ApplicationCvMeta | null> =>
    apiClient
      .get<ApplicationCvMeta>(`/applications/${appId}/cv/meta`)
      .then((r) => r.data)
      .catch((e) => {
        if ((e as { response?: { status?: number } })?.response?.status === 404) return null
        throw e
      }),

  upload: (appId: string, file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return apiClient
      .post<ApplicationCvMeta>(`/applications/${appId}/cv/upload`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data)
  },

  download: (appId: string) =>
    apiClient
      .get<Blob>(`/applications/${appId}/cv/download`, { responseType: 'blob' })
      .then((r) => r.data),

  delete: (appId: string) =>
    apiClient.delete(`/applications/${appId}/cv`).then(() => undefined),
}
