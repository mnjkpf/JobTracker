import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { applicationsApi } from './api'
import type { Application, CreateApplicationRequest, Page, UpdateStatusRequest } from './types'

const APPLICATIONS_KEY = ['applications'] as const

function errorMessage(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { detail?: string } } }
  return e?.response?.data?.detail ?? fallback
}

export const useApplications = () => {
  return useQuery({
    queryKey: APPLICATIONS_KEY,
    queryFn: () => applicationsApi.list({ page: 0, size: 100, sort: 'updatedAt,desc' }),
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
      // Roll back to the pre-mutation snapshot.
      context?.previous?.forEach(([queryKey, data]) => {
        queryClient.setQueryData(queryKey, data)
      })
      toast.error(errorMessage(error, 'Failed to update status'))
    },

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: APPLICATIONS_KEY })
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
