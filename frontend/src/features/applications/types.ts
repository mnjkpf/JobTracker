export type ApplicationStatus =
  | 'SAVED'
  | 'APPLIED'
  | 'SCREENING'
  | 'INTERVIEW'
  | 'FINAL'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'GHOSTED'

// Full backend enum sets (list responses may return any of these).
export type Seniority = 'INTERN' | 'JUNIOR' | 'JUNIOR_PLUS' | 'MID' | 'SENIOR' | 'LEAD' | 'NOT_SPECIFIED'
export type WorkMode = 'ONSITE' | 'HYBRID' | 'REMOTE' | 'NOT_SPECIFIED'
export type ContractType = 'UOP' | 'B2B' | 'UZ' | 'UMOWA_O_DZIELO' | 'NOT_SPECIFIED'

export interface Application {
  id: string
  name: string
  companyName: string | null
  location: string | null
  status: ApplicationStatus
  seniority: Seniority
  workMode: WorkMode
  contractType: ContractType
  url: string
  salaryMin: number | null
  salaryMax: number | null
  salaryCurrency: string | null
  appliedAt: string | null
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
}

export interface UpdateStatusRequest {
  status: ApplicationStatus
  note?: string
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

/**
 * Mirror of the backend ApplicationStateMachine. Kept in sync with
 * ApplicationStateMachine.java — OFFER is terminal (no outgoing transitions),
 * despite what older API drafts said.
 */
export const VALID_TRANSITIONS: Record<ApplicationStatus, ApplicationStatus[]> = {
  SAVED: ['APPLIED', 'WITHDRAWN'],
  APPLIED: ['SCREENING', 'REJECTED', 'WITHDRAWN', 'GHOSTED'],
  SCREENING: ['INTERVIEW', 'REJECTED', 'WITHDRAWN', 'GHOSTED'],
  INTERVIEW: ['FINAL', 'REJECTED', 'WITHDRAWN', 'GHOSTED'],
  FINAL: ['OFFER', 'REJECTED', 'WITHDRAWN', 'GHOSTED'],
  OFFER: [],
  REJECTED: [],
  WITHDRAWN: [],
  GHOSTED: [],
}

export const isValidTransition = (from: ApplicationStatus, to: ApplicationStatus): boolean =>
  VALID_TRANSITIONS[from].includes(to)
