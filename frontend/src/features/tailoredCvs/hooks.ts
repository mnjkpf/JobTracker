import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { tailoredCvsApi } from './api'
import type { GenerateTailoredCvRequest } from './api'

function errorMessage(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { detail?: string } } }
  return e?.response?.data?.detail ?? fallback
}

export const useTailoredCvs = (applicationId: string) =>
  useQuery({
    queryKey: ['tailored-cvs', applicationId],
    queryFn: () => tailoredCvsApi.list(applicationId),
  })

export const useTailoredCv = (applicationId: string, id: string | null) =>
  useQuery({
    queryKey: ['tailored-cv', applicationId, id],
    queryFn: () => tailoredCvsApi.getById(applicationId, id as string),
    enabled: !!id,
  })

export const useAtsScore = (applicationId: string, tailoredCvId: string) =>
  useQuery({
    queryKey: ['ats-score', applicationId, tailoredCvId],
    queryFn: () => tailoredCvsApi.getAtsScore(applicationId, tailoredCvId),
    enabled: !!tailoredCvId,
    staleTime: 0,
    retry: false,
  })

export const useGenerateTailoredCv = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: GenerateTailoredCvRequest) => tailoredCvsApi.generate(applicationId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tailored-cvs', applicationId] })
      toast.success('Tailored CV generated')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to generate tailored CV')),
  })
}

export const useDeleteTailoredCv = (applicationId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => tailoredCvsApi.delete(applicationId, id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tailored-cvs', applicationId] })
      toast.success('Tailored CV deleted')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to delete tailored CV')),
  })
}

export const useDownloadDocx = (applicationId: string) =>
  useMutation({
    mutationFn: async ({ id, version }: { id: string; version: number }) => {
      const blob = await tailoredCvsApi.downloadDocx(applicationId, id)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `tailored-cv-v${version}.docx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to download DOCX')),
  })
