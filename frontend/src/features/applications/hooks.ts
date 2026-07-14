import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { applicationsApi, gapAnalysisApi, type ListParams } from './api'
import type {
  Application,
  CreateApplicationRequest,
  GapAnalysis,
  Page,
  UpdateApplicationRequest,
  UpdateStatusRequest,
} from './types'

const APPLICATIONS_KEY = ['applications'] as const

function statusOf(error: unknown): number | undefined {
  return (error as { response?: { status?: number } })?.response?.status
}

function errorMessage(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { detail?: string } } }
  return e?.response?.data?.detail ?? fallback
}

export const useApplications = (filters: Pick<ListParams, 'q' | 'statuses'> = {}) => {
  return useQuery({
    queryKey: [...APPLICATIONS_KEY, filters],
    queryFn: () =>
      applicationsApi.list({ page: 0, size: 100, sort: 'updatedAt,desc', ...filters }),
  })
}

export const useApplication = (id: string) => {
  return useQuery({
    queryKey: ['application', id],
    queryFn: () => applicationsApi.getById(id),
    enabled: !!id,
  })
}

export const useCreateApplication = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateApplicationRequest) => applicationsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: APPLICATIONS_KEY })
      toast.success('Application created')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to create application')),
  })
}

export const useUpdateApplication = (id: string) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateApplicationRequest) => applicationsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: APPLICATIONS_KEY })
      queryClient.invalidateQueries({ queryKey: ['application', id] })
      toast.success('Application updated')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to update application')),
  })
}

export const useUpdateStatus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateStatusRequest }) =>
      applicationsApi.updateStatus(id, data),

    // Optimistic update — move the card instantly.
    onMutate: async ({ id, data }) => {
      await queryClient.cancelQueries({ queryKey: APPLICATIONS_KEY })
      const previous = queryClient.getQueriesData<Page<Application>>({ queryKey: APPLICATIONS_KEY })

      queryClient.setQueriesData<Page<Application>>({ queryKey: APPLICATIONS_KEY }, (old) => {
        if (!old) return old
        return {
          ...old,
          content: old.content.map((app) =>
            app.id === id ? { ...app, status: data.status } : app,
          ),
        }
      })

      return { previous }
    },

    onError: (error, _variables, context) => {
      context?.previous?.forEach(([queryKey, data]) => {
        queryClient.setQueryData(queryKey, data)
      })
      toast.error(errorMessage(error, 'Failed to update status'))
    },

    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: APPLICATIONS_KEY })
      queryClient.invalidateQueries({ queryKey: ['application', variables.id] })
    },
  })
}

export const useDeleteApplication = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => applicationsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: APPLICATIONS_KEY })
      toast.success('Application deleted')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to delete application')),
  })
}

export const useGapAnalysis = (applicationId: string) => {
  return useQuery({
    queryKey: ['gap-analysis', applicationId],
    queryFn: () => gapAnalysisApi.get(applicationId),
    enabled: !!applicationId,
    retry: (failureCount, error) => {
      const s = statusOf(error)
      if (s === 404 || s === 422) return false // no analysis / no CV — expected
      return failureCount < 1
    },
  })
}

export const useRunGapAnalysis = (applicationId: string) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => gapAnalysisApi.run(applicationId),
    onSuccess: (data: GapAnalysis) => {
      queryClient.setQueryData(['gap-analysis', applicationId], data)
      toast.success('Gap analysis complete')
    },
    onError: (error) => toast.error(errorMessage(error, 'Gap analysis failed')),
  })
}
