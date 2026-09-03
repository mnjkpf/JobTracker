import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { applicationCvApi } from './api'

const CV_KEY = (appId: string) => ['application-cv', appId]

function errorMessage(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { detail?: string } } }
  return e?.response?.data?.detail ?? fallback
}

export const useApplicationCv = (appId: string) =>
  useQuery({
    queryKey: CV_KEY(appId),
    queryFn: () => applicationCvApi.get(appId),
    enabled: !!appId,
  })

export const useUploadApplicationCv = (appId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => applicationCvApi.upload(appId, file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CV_KEY(appId) })
      toast.success('CV attached')
    },
    onError: (e) => toast.error(errorMessage(e, 'Failed to attach CV')),
  })
}

export const useDeleteApplicationCv = (appId: string) => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => applicationCvApi.delete(appId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CV_KEY(appId) })
      toast.success('CV removed')
    },
    onError: (e) => toast.error(errorMessage(e, 'Failed to remove CV')),
  })
}

export const useDownloadApplicationCv = (appId: string) =>
  useMutation({
    mutationFn: async (fileName: string) => {
      const blob = await applicationCvApi.download(appId)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    },
    onError: (e) => toast.error(errorMessage(e, 'Failed to download CV')),
  })
