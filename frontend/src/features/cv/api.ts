import { apiClient } from '@/api/client'
import type {
  AddSkillRequest,
  Education,
  EducationRequest,
  Experience,
  ExperienceRequest,
  Language,
  LanguageRequest,
  MasterCv,
  Project,
  ProjectRequest,
  Skill,
  UpdateProfileRequest,
  UpdateSkillRequest,
} from './types'

// Reality check: backend CvController maps "/api/v1/cv" as base.
// - GET/PATCH "/me" -> /cv/me (NOT /cv/me/profile as originally assumed)
// - "upload" is mounted directly under "/cv", NOT under "/cv/me" -> /cv/upload
export const cvApi = {
  getMy: () => apiClient.get<MasterCv>('/cv/me').then((r) => r.data),

  updateProfile: (data: UpdateProfileRequest) =>
    apiClient.patch<MasterCv>('/cv/me', data).then((r) => r.data),

  uploadCv: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient
      .post<MasterCv>('/cv/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data)
  },

  // Experiences
  addExperience: (data: ExperienceRequest) =>
    apiClient.post<Experience>('/cv/me/experiences', data).then((r) => r.data),
  updateExperience: (id: string, data: ExperienceRequest) =>
    apiClient.patch<Experience>(`/cv/me/experiences/${id}`, data).then((r) => r.data),
  deleteExperience: (id: string) =>
    apiClient.delete(`/cv/me/experiences/${id}`).then(() => {}),

  // Educations
  addEducation: (data: EducationRequest) =>
    apiClient.post<Education>('/cv/me/educations', data).then((r) => r.data),
  updateEducation: (id: string, data: EducationRequest) =>
    apiClient.patch<Education>(`/cv/me/educations/${id}`, data).then((r) => r.data),
  deleteEducation: (id: string) =>
    apiClient.delete(`/cv/me/educations/${id}`).then(() => {}),

  // Skills
  addSkill: (data: AddSkillRequest) =>
    apiClient.post<Skill>('/cv/me/skills', data).then((r) => r.data),
  updateSkill: (id: string, data: UpdateSkillRequest) =>
    apiClient.patch<Skill>(`/cv/me/skills/${id}`, data).then((r) => r.data),
  deleteSkill: (id: string) => apiClient.delete(`/cv/me/skills/${id}`).then(() => {}),

  // Projects
  addProject: (data: ProjectRequest) =>
    apiClient.post<Project>('/cv/me/projects', data).then((r) => r.data),
  updateProject: (id: string, data: ProjectRequest) =>
    apiClient.patch<Project>(`/cv/me/projects/${id}`, data).then((r) => r.data),
  deleteProject: (id: string) => apiClient.delete(`/cv/me/projects/${id}`).then(() => {}),

  // Languages
  addLanguage: (data: LanguageRequest) =>
    apiClient.post<Language>('/cv/me/languages', data).then((r) => r.data),
  updateLanguage: (id: string, data: LanguageRequest) =>
    apiClient.patch<Language>(`/cv/me/languages/${id}`, data).then((r) => r.data),
  deleteLanguage: (id: string) => apiClient.delete(`/cv/me/languages/${id}`).then(() => {}),
}
