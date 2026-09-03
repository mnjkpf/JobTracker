import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { statusesApi, type CreateStatusPayload, type Status, type UpdateStatusPayload } from './api'

const STATUSES_KEY = ['statuses'] as const
const APPLICATIONS_KEY = ['applications'] as const

function errorMessage(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { detail?: string } } }
  return e?.response?.data?.detail ?? fallback
}

export const useStatuses = () =>
  useQuery({
    queryKey: STATUSES_KEY,
    queryFn: () => statusesApi.list(),
    staleTime: 5 * 60 * 1000, // statuses change rarely
  })

export const useCreateStatus = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateStatusPayload) => statusesApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: STATUSES_KEY })
      toast.success('Status added')
    },
    onError: (e) => toast.error(errorMessage(e, 'Failed to add status')),
  })
}

export const useUpdateStatusCategory = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateStatusPayload }) =>
      statusesApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: STATUSES_KEY })
      qc.invalidateQueries({ queryKey: APPLICATIONS_KEY })
    },
    onError: (e) => toast.error(errorMessage(e, 'Failed to update status')),
  })
}

export const useDeleteStatus = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, reassignTo }: { id: string; reassignTo?: string }) =>
      statusesApi.remove(id, reassignTo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: STATUSES_KEY })
      qc.invalidateQueries({ queryKey: APPLICATIONS_KEY })
      toast.success('Status deleted')
    },
    onError: (e) => toast.error(errorMessage(e, 'Failed to delete status')),
  })
}

export const useReorderStatuses = () => {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (orderedIds: string[]) => statusesApi.reorder(orderedIds),
    // Server returns the reordered list — write it straight into cache.
    onMutate: async (orderedIds) => {
      await qc.cancelQueries({ queryKey: STATUSES_KEY })
      const previous = qc.getQueryData<Status[]>(STATUSES_KEY)
      if (previous) {
        const byId = new Map(previous.map((s) => [s.id, s]))
        const next = orderedIds
          .map((id, i) => {
            const s = byId.get(id)
            return s ? { ...s, position: i } : null
          })
          .filter((s): s is Status => s !== null)
        qc.setQueryData(STATUSES_KEY, next)
      }
      return { previous }
    },
    onError: (e, _ids, ctx) => {
      if (ctx?.previous) qc.setQueryData(STATUSES_KEY, ctx.previous)
      toast.error(errorMessage(e, 'Failed to reorder statuses'))
    },
    onSuccess: (data) => qc.setQueryData(STATUSES_KEY, data),
  })
}
