// Semantic marker on a status (mirrors backend SystemStatusType). Custom
// user-created statuses have systemType === null.
export type SystemStatusType =
  | 'SAVED'
  | 'APPLIED'
  | 'SCREENING'
  | 'INTERVIEW'
  | 'FINAL'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'GHOSTED'

/** Status of an application as returned embedded in ApplicationResponse. */
export interface ApplicationStatusRef {
  id: string
  name: string
  color: string // hex, e.g. "#3b82f6"
  systemType: SystemStatusType | null
}

// These enums are unchanged on the backend.
export type Seniority = 'INTERN' | 'JUNIOR' | 'JUNIOR_PLUS' | 'MID' | 'SENIOR' | 'LEAD' | 'NOT_SPECIFIED'
export type WorkMode = 'ONSITE' | 'HYBRID' | 'REMOTE' | 'NOT_SPECIFIED'
export type ContractType = 'UOP' | 'B2B' | 'UZ' | 'UMOWA_O_DZIELO' | 'NOT_SPECIFIED'

export interface Application {
  id: string
  name: string
  companyName: string | null
  location: string | null
  status: ApplicationStatusRef
  seniority: Seniority
  workMode: WorkMode
  contractType: ContractType
  url: string
  description?: string | null
  salaryMin: number | null
  salaryMax: number | null
  salaryCurrency: string | null
  appliedAt: string | null
  archived: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateApplicationRequest {
  name: string
  companyName?: string
  url: string
  description: string
  location?: string
  seniority: Seniority
  workMode: WorkMode
  contractType: ContractType
  // Optional; backend defaults to the user's SAVED-type status when omitted.
  statusId?: string
}

export interface UpdateStatusRequest {
  statusId: string
  note?: string
}

export interface UpdateApplicationRequest {
  name?: string
  description?: string
  location?: string
  contractType?: ContractType
  seniority?: Seniority
  workMode?: WorkMode
  salaryMin?: number
  salaryMax?: number
  salaryCurrency?: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

// ─── Gap analysis — POST/GET /applications/{id}/gap-analysis ──────────
export interface SkillMatch {
  requiredSkillName: string
  requiredSkillId: string
  matched: boolean
  matchedSkillName: string | null
  similarity: number
  required: boolean
}

export interface GapAnalysis {
  score: number
  totalRequiredSkills: number
  matchedRequiredSkills: number
  missedRequiredSkills: number
  analyzeAt: string
  matchedSkills: SkillMatch[]
  missingSkills: SkillMatch[]
}
