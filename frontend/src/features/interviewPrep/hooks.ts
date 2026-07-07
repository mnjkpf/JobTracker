import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { interviewNotesApi, interviewPrepApi } from './api'
import type { CreateNoteRequest, ReflectionRequest, UpdateNoteRequest } from './api'

function statusOf(error: unknown): number | undefined {
  return (error as { response?: { status?: number } })?.response?.status
}

function errorMessage(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { detail?: string } } }
  return e?.response?.data?.detail ?? fallback
}

const PREP_KEY = (id: string) => ['interview-prep', id]
const NOTES_KEY = (id: string) => ['interview-notes', id]
const RELEVANT_KEY = (id: string) => ['relevant-notes', id]

export const useInterviewPrep = (applicationId: string) =>
  useQuery({
    queryKey: PREP_KEY(applicationId),
    queryFn: () => interviewPrepApi.get(applicationId),
    retry: (failureCount, error) => {
      if (statusOf(error) === 404) return false // no prep yet — expected until INTERVIEW status
      return failureCount < 2
    },
  })

export const useRelevantNotes = (applicationId: string) =>
  useQuery({
    queryKey: RELEVANT_KEY(applicationId),
    queryFn: () => interviewPrepApi.getRelevantNotes(applicationId),
  })

export const useGeneratePrep = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => interviewPrepApi.generate(applicationId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: PREP_KEY(applicationId) })
      qc.invalidateQueries({ queryKey: RELEVANT_KEY(applicationId) })
      toast.success('Prep guide generated')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to generate prep guide')),
  })
}

export const useDeletePrep = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => interviewPrepApi.delete(applicationId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: PREP_KEY(applicationId) })
      qc.invalidateQueries({ queryKey: NOTES_KEY(applicationId) })
      toast.success('Prep deleted')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to delete prep')),
  })
}

export const useInterviewNotes = (applicationId: string, enabled = true) =>
  useQuery({
    queryKey: NOTES_KEY(applicationId),
    queryFn: () => interviewNotesApi.list(applicationId),
    enabled,
  })

export const useCreateNote = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateNoteRequest) => interviewNotesApi.create(applicationId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: NOTES_KEY(applicationId) })
      toast.success('Note added')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to save note')),
  })
}

export const useUpdateNote = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ noteId, data }: { noteId: string; data: UpdateNoteRequest }) =>
      interviewNotesApi.update(applicationId, noteId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: NOTES_KEY(applicationId) })
      toast.success('Note updated')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to update note')),
  })
}

export const useDeleteNote = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (noteId: string) => interviewNotesApi.delete(applicationId, noteId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: NOTES_KEY(applicationId) })
      toast.success('Note deleted')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to delete note')),
  })
}

export const useAddReflection = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: ReflectionRequest) => interviewNotesApi.addReflection(applicationId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: NOTES_KEY(applicationId) })
      toast.success('Reflection saved')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to save reflection')),
  })
}
