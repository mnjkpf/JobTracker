import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { coverLettersApi } from './api'
import type { GenerateCoverLetterRequest, RefineCoverLetterRequest } from './api'

function errorMessage(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { detail?: string } } }
  return e?.response?.data?.detail ?? fallback
}

export const useCoverLetters = (applicationId: string) =>
  useQuery({
    queryKey: ['cover-letters', applicationId],
    queryFn: () => coverLettersApi.list(applicationId),
  })

export const useCoverLetter = (applicationId: string, id: string | null) =>
  useQuery({
    queryKey: ['cover-letter', applicationId, id],
    queryFn: () => coverLettersApi.getById(applicationId, id as string),
    enabled: !!id,
  })

export const useGenerateCoverLetter = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: GenerateCoverLetterRequest) => coverLettersApi.generate(applicationId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cover-letters', applicationId] })
      toast.success('Cover letter generated')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to generate cover letter')),
  })
}

export const useRefineCoverLetter = (applicationId: string, id: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RefineCoverLetterRequest) => coverLettersApi.refine(applicationId, id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cover-letters', applicationId] })
      toast.success('Refined version created')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to refine cover letter')),
  })
}

export const useDeleteCoverLetter = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => coverLettersApi.delete(applicationId, id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cover-letters', applicationId] })
      toast.success('Cover letter deleted')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to delete cover letter')),
  })
}
