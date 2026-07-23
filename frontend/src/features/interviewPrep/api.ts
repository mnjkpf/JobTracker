import { apiClient } from '@/api/client'

export type PrepStatus = 'DRAFT' | 'GENERATED' | 'COMPLETED'
export type QuestionCategory = 'TECHNICAL' | 'BEHAVIORAL' | 'QUESTION_TO_ASK'
export type NoteType = 'PREP_NOTE' | 'POST_INTERVIEW'

export interface InterviewQuestion {
  id: string
  category: QuestionCategory
  question: string
  suggestedAnswer: string | null
  displayOrder: number
}

// Backend's `relevantPastNotes` field on this DTO is never populated (checked
// service code — no setter call exists), so it's omitted here. RAG transparency
// comes from the dedicated /relevant-notes endpoint instead.
export interface InterviewPrep {
  id: string
  applicationId: string
  status: PrepStatus
  promptVersion: string | null
  createdAt: string
  technicalQuestions: InterviewQuestion[]
  behavioralQuestions: InterviewQuestion[]
  questionsToAsk: InterviewQuestion[]
}

export interface RelevantNote {
  id: string
  content: string
  noteType: NoteType
  applicationId: string
  similarity: number
}

export interface InterviewNote {
  id: string
  interviewPrepId: string
  content: string
  noteType: NoteType
  createdAt: string
  updatedAt: string
}

export interface CreateNoteRequest {
  content: string
  noteType: NoteType
}

export interface UpdateNoteRequest {
  content: string
}

export interface ReflectionRequest {
  content: string
  questionsAsked?: string[]
}

export const interviewPrepApi = {
  get: (applicationId: string) =>
    apiClient.get<InterviewPrep>(`/applications/${applicationId}/interview-prep`).then((r) => r.data),

  generate: (applicationId: string) =>
    apiClient
      .post<InterviewPrep>(`/applications/${applicationId}/interview-prep/generate`)
      .then((r) => r.data),

  delete: (applicationId: string) =>
    apiClient.delete(`/applications/${applicationId}/interview-prep`).then(() => undefined),

  getRelevantNotes: (applicationId: string) =>
    apiClient
      .get<RelevantNote[]>(`/applications/${applicationId}/interview-prep/relevant-notes`)
      .then((r) => r.data),
}

export const interviewNotesApi = {
  list: (applicationId: string) =>
    apiClient
      .get<InterviewNote[]>(`/applications/${applicationId}/interview-prep/notes`)
      .then((r) => r.data),

  create: (applicationId: string, data: CreateNoteRequest) =>
    apiClient
      .post<InterviewNote>(`/applications/${applicationId}/interview-prep/notes`, data)
      .then((r) => r.data),

  update: (applicationId: string, noteId: string, data: UpdateNoteRequest) =>
    apiClient
      .put<InterviewNote>(`/applications/${applicationId}/interview-prep/notes/${noteId}`, data)
      .then((r) => r.data),

  delete: (applicationId: string, noteId: string) =>
    apiClient.delete(`/applications/${applicationId}/interview-prep/notes/${noteId}`).then(() => undefined),

  addReflection: (applicationId: string, data: ReflectionRequest) =>
    apiClient
      .post<InterviewNote>(`/applications/${applicationId}/interview-prep/notes/reflection`, data)
      .then((r) => r.data),
}
