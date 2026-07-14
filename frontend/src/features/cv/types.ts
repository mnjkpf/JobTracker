export type CvLanguage = 'EN' | 'PL'
export type SkillCategory = 'LANGUAGE' | 'FRAMEWORK' | 'DATABASE' | 'TOOL' | 'SOFT' | 'OTHER'
export type SkillLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT'
export type LanguageLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2' | 'NATIVE'

export interface Experience {
  id: string
  position: string
  company: string
  location: string | null
  startDate: string
  endDate: string | null // null = current
  description: string | null
}

export interface Education {
  id: string
  institution: string
  degree: string | null
  fieldOfStudy: string | null
  location: string | null
  startDate: string | null
  endDate: string | null
  description: string | null
}

export interface Skill {
  id: string // MasterCvSkill id — use for update/delete
  skillId: string // catalog Skill id
  name: string
  category: SkillCategory | null
  level: SkillLevel | null
}

export interface Project {
  id: string
  name: string
  description: string | null
  url: string | null
  technologies: string | null
  startDate: string | null
  endDate: string | null
}

export interface Language {
  id: string
  name: string
  level: LanguageLevel | null
}

export interface MasterCv {
  id: string
  fullName: string
  headline: string
  email: string
  phone: string | null
  linkedInUrl: string | null
  githubUrl: string | null
  summary: string
  language: CvLanguage
  updatedAt: string
  experiences: Experience[]
  educations: Education[]
  skills: Skill[]
  projects: Project[]
  languages: Language[]
}

// ─── Request payloads (mirror backend DTO validation) ──────────────────

export interface UpdateProfileRequest {
  fullName?: string
  headline?: string
  email?: string
  phone?: string
  linkedInUrl?: string
  githubUrl?: string
  summary?: string
  language?: CvLanguage
}

export interface ExperienceRequest {
  position?: string
  company?: string
  location?: string
  startDate?: string
  endDate?: string | null
  description?: string
}

export interface EducationRequest {
  institution?: string
  degree?: string
  fieldOfStudy?: string
  location?: string
  startDate?: string | null
  endDate?: string | null
  description?: string
}

export interface AddSkillRequest {
  name: string
  category?: SkillCategory
  level?: SkillLevel
}

export interface UpdateSkillRequest {
  level?: SkillLevel
}

export interface ProjectRequest {
  name?: string
  description?: string
  url?: string
  technologies?: string
  startDate?: string | null
  endDate?: string | null
}

export interface LanguageRequest {
  name?: string
  level?: LanguageLevel
}
