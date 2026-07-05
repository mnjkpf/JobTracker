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
import { useCreateApplication } from './hooks'

const schema = z.object({
  name: z.string().min(3, 'At least 3 characters'),
  companyName: z.string().optional(),
  url: z.string().url('Must be a valid URL'),
  description: z.string().min(20, 'At least 20 characters'),
  location: z.string().optional(),
  seniority: z.enum(['JUNIOR', 'MID', 'SENIOR', 'LEAD']),
  workMode: z.enum(['ONSITE', 'HYBRID', 'REMOTE']),
  contractType: z.enum(['UOP', 'B2B', 'UZ']),
})

type Values = z.infer<typeof schema>

const SENIORITIES = ['JUNIOR', 'MID', 'SENIOR', 'LEAD'] as const
const WORK_MODES = ['ONSITE', 'HYBRID', 'REMOTE'] as const
const CONTRACT_TYPES = ['UOP', 'B2B', 'UZ'] as const

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
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function CreateApplicationModal({ open, onOpenChange }: Props) {
  const create = useCreateApplication()
  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      companyName: '',
      url: '',
      description: '',
      location: '',
      seniority: 'JUNIOR',
      workMode: 'HYBRID',
      contractType: 'UOP',
    },
  })

  const onSubmit = (values: Values) => {
    create.mutate(values, {
      onSuccess: () => {
        reset()
        onOpenChange(false)
      },
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>New application</DialogTitle>
          <DialogDescription>Track a new job you're applying to.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <Field label="Position" error={errors.name?.message}>
            <Input {...register('name')} placeholder="Junior Java Backend Developer" />
          </Field>
          <Field label="Company" error={errors.companyName?.message}>
            <Input {...register('companyName')} placeholder="Allegro" />
          </Field>
          <Field label="Job URL" error={errors.url?.message}>
            <Input {...register('url')} placeholder="https://..." />
          </Field>
          <Field label="Description" error={errors.description?.message}>
            <Textarea {...register('description')} rows={4} placeholder="Paste the job description..." />
          </Field>
          <Field label="Location" error={errors.location?.message}>
            <Input {...register('location')} placeholder="Warsaw" />
          </Field>

          <div className="grid grid-cols-3 gap-3">
            <SelectField control={control} name="seniority" label="Seniority" options={SENIORITIES} />
            <SelectField control={control} name="workMode" label="Work mode" options={WORK_MODES} />
            <SelectField
              control={control}
              name="contractType"
              label="Contract"
              options={CONTRACT_TYPES}
            />
          </div>

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={create.isPending}>
              {create.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Create
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
