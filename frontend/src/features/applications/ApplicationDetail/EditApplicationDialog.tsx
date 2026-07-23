import type { ReactNode } from 'react'
import { Controller, useForm, type Control } from 'react-hook-form'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useUpdateApplication } from '../hooks'
import type { Application } from '../types'

const SENIORITIES = ['INTERN', 'JUNIOR', 'JUNIOR_PLUS', 'MID', 'SENIOR', 'LEAD', 'NOT_SPECIFIED'] as const
const WORK_MODES = ['ONSITE', 'HYBRID', 'REMOTE', 'NOT_SPECIFIED'] as const
const CONTRACT_TYPES = ['UOP', 'B2B', 'UZ', 'UMOWA_O_DZIELO', 'NOT_SPECIFIED'] as const

const schema = z.object({
  name: z.string().min(3, 'At least 3 characters'),
  location: z.string().optional(),
  description: z.string().optional(),
  seniority: z.enum(SENIORITIES),
  workMode: z.enum(WORK_MODES),
  contractType: z.enum(CONTRACT_TYPES),
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

function SelectField({
  control,
  name,
  label,
  options,
}: {
  control: Control<Values>
  name: 'seniority' | 'workMode' | 'contractType'
  label: string
  options: readonly string[]
}) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      <Controller
        control={control}
        name={name}
        render={({ field }) => (
          <Select value={field.value} onValueChange={(v) => field.onChange(v)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {options.map((o) => (
                <SelectItem key={o} value={o}>
                  {o}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      />
    </div>
  )
}

interface Props {
  application: Application
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function EditApplicationDialog({ application, open, onOpenChange }: Props) {
  const update = useUpdateApplication(application.id)
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: application.name,
      location: application.location ?? '',
      description: application.description ?? '',
      seniority: application.seniority,
      workMode: application.workMode,
      contractType: application.contractType,
    },
  })

  const onSubmit = (values: Values) => {
    update.mutate(values, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Edit application</DialogTitle>
          <DialogDescription>Update details. Status is changed from the Overview tab.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <Field label="Position" error={errors.name?.message}>
            <Input {...register('name')} />
          </Field>
          <Field label="Location" error={errors.location?.message}>
            <Input {...register('location')} />
          </Field>
          <Field label="Description" error={errors.description?.message}>
            <Textarea {...register('description')} rows={4} />
          </Field>
          <div className="grid grid-cols-3 gap-3">
            <SelectField control={control} name="seniority" label="Seniority" options={SENIORITIES} />
            <SelectField control={control} name="workMode" label="Work mode" options={WORK_MODES} />
            <SelectField control={control} name="contractType" label="Contract" options={CONTRACT_TYPES} />
          </div>

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={update.isPending}>
              {update.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Save
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
