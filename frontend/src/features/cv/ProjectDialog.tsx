import { useEffect, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useAddProject, useUpdateProject } from './hooks'
import type { Project } from './types'

const schema = z.object({
  name: z.string().min(1, 'Required').max(255),
  description: z.string().max(4000).optional(),
  url: z.string().max(500).optional(),
  technologies: z.string().max(1000).optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
})

type Values = z.infer<typeof schema>

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      {children}
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  )
}

interface Props {
  project: Project | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function ProjectDialog({ project, open, onOpenChange }: Props) {
  const isEdit = !!project
  const add = useAddProject()
  const update = useUpdateProject(project?.id ?? '')
  const mutation = isEdit ? update : add

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: project?.name ?? '',
      description: project?.description ?? '',
      url: project?.url ?? '',
      technologies: project?.technologies ?? '',
      startDate: project?.startDate ?? '',
      endDate: project?.endDate ?? '',
    },
  })

  useEffect(() => {
    if (open) {
      reset({
        name: project?.name ?? '',
        description: project?.description ?? '',
        url: project?.url ?? '',
        technologies: project?.technologies ?? '',
        startDate: project?.startDate ?? '',
        endDate: project?.endDate ?? '',
      })
    }
  }, [open, project, reset])

  const onSubmit = (values: Values) => {
    // Empty date inputs are "" — the backend can't parse "" as LocalDate, so omit instead.
    const payload = {
      ...values,
      startDate: values.startDate || undefined,
      endDate: values.endDate || undefined,
    }
    mutation.mutate(payload, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Edit project' : 'Add project'}</DialogTitle>
          <DialogDescription>A project you've built, shown on your CV.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <Field label="Name" error={errors.name?.message}>
            <Input {...register('name')} placeholder="JobTracker" />
          </Field>
          <Field label="Description" error={errors.description?.message}>
            <Textarea {...register('description')} rows={3} placeholder="AI-powered..." />
          </Field>
          <Field label="Technologies" error={errors.technologies?.message}>
            <Input {...register('technologies')} placeholder="Java, Spring Boot" />
          </Field>
          <Field label="URL" error={errors.url?.message}>
            <Input {...register('url')} placeholder="https://github.com/..." />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Start date" error={errors.startDate?.message}>
              <Input type="date" {...register('startDate')} />
            </Field>
            <Field label="End date" error={errors.endDate?.message}>
              <Input type="date" {...register('endDate')} />
            </Field>
          </div>

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Save
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
