import { apiClient } from '@/api/client'

export type CvLanguage = 'EN' | 'PL'

export interface TailoredCvSummary {
  id: string
  version: number
  headline: string
  language: CvLanguage
  createdAt: string
}

export interface TailoredExperience {
  id: string
  position: string
  company: string
  location: string | null
  startDate: string
  endDate: string | null
  description: string | null
}

export interface TailoredEducation {
  id: string
  institution: string
  degree: string | null
  fieldOfStudy: string | null
  location: string | null
  startDate: string | null
  endDate: string | null
  description: string | null
}

export interface TailoredSkill {
  id: string
  name: string
  category: string | null
  level: string | null
}

export interface TailoredProject {
  id: string
  name: string
  description: string | null
  url: string | null
  technologies: string | null
  startDate: string | null
  endDate: string | null
}

export interface TailoredLanguage {
  id: string
  name: string
  level: string | null
}

export interface TailoredCv {
  id: string
  applicationId: string
  version: number
  fullName: string
  headline: string
  email: string
  phone: string | null
  linkedInUrl: string | null
  githubUrl: string | null
  summary: string
  language: CvLanguage
  createdAt: string
  experiences: TailoredExperience[]
  educations: TailoredEducation[]
  skills: TailoredSkill[]
  projects: TailoredProject[]
  languages: TailoredLanguage[]
}

// Backend request only accepts optional emphasisInstructions — language is server-decided.
export interface GenerateTailoredCvRequest {
  emphasisInstructions?: string
}

export interface AtsScore {
  score: number // 0-100 integer
  totalKeywords: number
  matchedCount: number
  matched: string[]
  missing: string[]
  analyzedAt: string
}

export const tailoredCvsApi = {
  list: (applicationId: string) =>
    apiClient
      .get<TailoredCvSummary[]>(`/applications/${applicationId}/tailored-cvs`)
      .then((r) => r.data),

  getById: (applicationId: string, id: string) =>
    apiClient
      .get<TailoredCv>(`/applications/${applicationId}/tailored-cvs/${id}`)
      .then((r) => r.data),

  generate: (applicationId: string, data: GenerateTailoredCvRequest) =>
    apiClient
      .post<TailoredCv>(`/applications/${applicationId}/tailored-cvs`, data)
      .then((r) => r.data),

  getAtsScore: (applicationId: string, id: string) =>
    apiClient
      .get<AtsScore>(`/applications/${applicationId}/tailored-cvs/${id}/ats-score`)
      .then((r) => r.data),

  downloadDocx: (applicationId: string, id: string) =>
    apiClient
      .get<Blob>(`/applications/${applicationId}/tailored-cvs/${id}/download`, {
        responseType: 'blob',
      })
      .then((r) => r.data),

  delete: (applicationId: string, id: string) =>
    apiClient.delete(`/applications/${applicationId}/tailored-cvs/${id}`).then(() => undefined),
}
