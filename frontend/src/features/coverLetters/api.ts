import { apiClient } from '@/api/client'

// Backend enum: FORMAL / CONVERSATIONAL / ENTHUSIASTIC (not PROFESSIONAL/FRIENDLY).
export type CoverLetterTone = 'FORMAL' | 'CONVERSATIONAL' | 'ENTHUSIASTIC'

export interface CoverLetterSummary {
  id: string
  version: number
  tone: CoverLetterTone
  contentPreview: string
  createdAt: string
}

export interface CoverLetter {
  id: string
  version: number
  tone: CoverLetterTone
  content: string
  createdAt: string
}

export interface GenerateCoverLetterRequest {
  tone: CoverLetterTone
}

export interface RefineCoverLetterRequest {
  instruction: string
}

export const coverLettersApi = {
  list: (applicationId: string) =>
    apiClient
      .get<CoverLetterSummary[]>(`/applications/${applicationId}/cover-letters`)
      .then((r) => r.data),

  getById: (applicationId: string, id: string) =>
    apiClient
      .get<CoverLetter>(`/applications/${applicationId}/cover-letters/${id}`)
      .then((r) => r.data),

  generate: (applicationId: string, data: GenerateCoverLetterRequest) =>
    apiClient
      .post<CoverLetter>(`/applications/${applicationId}/cover-letters`, data)
      .then((r) => r.data),

  refine: (applicationId: string, id: string, data: RefineCoverLetterRequest) =>
    apiClient
      .post<CoverLetter>(`/applications/${applicationId}/cover-letters/${id}/refine`, data)
      .then((r) => r.data),

  delete: (applicationId: string, id: string) =>
    apiClient.delete(`/applications/${applicationId}/cover-letters/${id}`).then(() => undefined),
}
