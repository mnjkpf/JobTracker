import { useEffect, useState, type ReactNode } from 'react'
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
import { useAddExperience, useUpdateExperience } from './hooks'
import type { Experience } from './types'

const schema = z.object({
  position: z.string().min(1, 'Required').max(255),
  company: z.string().min(1, 'Required').max(255),
  location: z.string().max(255).optional(),
  startDate: z.string().min(1, 'Required'),
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
  experience: Experience | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function ExperienceDialog({ experience, open, onOpenChange }: Props) {
  const isEdit = !!experience
  const add = useAddExperience()
  const update = useUpdateExperience(experience?.id ?? '')
  const mutation = isEdit ? update : add
  const [current, setCurrent] = useState(!experience?.endDate)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      position: experience?.position ?? '',
      company: experience?.company ?? '',
      location: experience?.location ?? '',
      startDate: experience?.startDate ?? '',
      endDate: experience?.endDate ?? '',
      description: experience?.description ?? '',
    },
  })

  useEffect(() => {
    if (open) {
      reset({
        position: experience?.position ?? '',
        company: experience?.company ?? '',
        location: experience?.location ?? '',
        startDate: experience?.startDate ?? '',
        endDate: experience?.endDate ?? '',
        description: experience?.description ?? '',
      })
      setCurrent(!experience?.endDate)
    }
  }, [open, experience, reset])

  const onSubmit = (values: Values) => {
    const payload = { ...values, endDate: current ? null : values.endDate || undefined }
    mutation.mutate(payload, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Edit experience' : 'Add experience'}</DialogTitle>
          <DialogDescription>A role you've held, shown on your CV.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Position" error={errors.position?.message}>
              <Input {...register('position')} placeholder="Backend Developer Intern" />
            </Field>
            <Field label="Company" error={errors.company?.message}>
              <Input {...register('company')} placeholder="Local Startup" />
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
              <Input type="date" {...register('endDate')} disabled={current} />
            </Field>
          </div>
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={current}
              onChange={(e) => setCurrent(e.target.checked)}
              className="h-4 w-4 rounded border-slate-300"
            />
            I currently work here
          </label>
          <Field label="Description" error={errors.description?.message}>
            <Textarea {...register('description')} rows={4} placeholder="Built REST API for internal admin panel..." />
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
