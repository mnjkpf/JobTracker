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
import { useAddEducation, useUpdateEducation } from './hooks'
import type { Education } from './types'

const schema = z.object({
  institution: z.string().min(1, 'Required').max(255),
  degree: z.string().max(100).optional(),
  fieldOfStudy: z.string().max(255).optional(),
  location: z.string().max(255).optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  description: z.string().max(4000).optional(),
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
  education: Education | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function EducationDialog({ education, open, onOpenChange }: Props) {
  const isEdit = !!education
  const add = useAddEducation()
  const update = useUpdateEducation(education?.id ?? '')
  const mutation = isEdit ? update : add

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      institution: education?.institution ?? '',
      degree: education?.degree ?? '',
      fieldOfStudy: education?.fieldOfStudy ?? '',
      location: education?.location ?? '',
      startDate: education?.startDate ?? '',
      endDate: education?.endDate ?? '',
      description: education?.description ?? '',
    },
  })

  useEffect(() => {
    if (open) {
      reset({
        institution: education?.institution ?? '',
        degree: education?.degree ?? '',
        fieldOfStudy: education?.fieldOfStudy ?? '',
        location: education?.location ?? '',
        startDate: education?.startDate ?? '',
        endDate: education?.endDate ?? '',
        description: education?.description ?? '',
      })
    }
  }, [open, education, reset])

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
          <DialogTitle>{isEdit ? 'Edit education' : 'Add education'}</DialogTitle>
          <DialogDescription>A degree or course, shown on your CV.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <Field label="Institution" error={errors.institution?.message}>
            <Input {...register('institution')} placeholder="WSB Warsaw" />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Degree" error={errors.degree?.message}>
              <Input {...register('degree')} placeholder="Bachelor" />
            </Field>
            <Field label="Field of study" error={errors.fieldOfStudy?.message}>
              <Input {...register('fieldOfStudy')} placeholder="Computer Science" />
            </Field>
          </div>
          <Field label="Location" error={errors.location?.message}>
            <Input {...register('location')} placeholder="Warsaw" />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Start date" error={errors.startDate?.message}>
              <Input type="date" {...register('startDate')} />
            </Field>
            <Field label="End date" error={errors.endDate?.message}>
              <Input type="date" {...register('endDate')} />
            </Field>
          </div>
          <Field label="Description" error={errors.description?.message}>
            <Textarea {...register('description')} rows={3} />
          </Field>

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
