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
import { useUpdateProfile } from './hooks'
import type { CvLanguage, MasterCv } from './types'

const LANGUAGES = ['EN', 'PL'] as const

const schema = z.object({
  fullName: z.string().min(1, 'Required').max(255),
  headline: z.string().min(1, 'Required').max(255),
  email: z.string().email('Must be a valid email').max(255),
  phone: z.string().max(50).optional(),
  linkedInUrl: z.string().max(500).optional(),
  githubUrl: z.string().max(500).optional(),
  summary: z.string().max(4000).optional(),
  language: z.enum(LANGUAGES),
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
  cv: Partial<MasterCv> | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function EditProfileDialog({ cv, open, onOpenChange }: Props) {
  const update = useUpdateProfile()
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    values: {
      fullName: cv?.fullName ?? '',
      headline: cv?.headline ?? '',
      email: cv?.email ?? '',
      phone: cv?.phone ?? '',
      linkedInUrl: cv?.linkedInUrl ?? '',
      githubUrl: cv?.githubUrl ?? '',
      summary: cv?.summary ?? '',
      language: (cv?.language as CvLanguage) ?? 'EN',
    },
  })

  const onSubmit = (values: Values) => {
    update.mutate(values, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Edit profile</DialogTitle>
          <DialogDescription>Your contact info and headline, shown at the top of your CV.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <Field label="Full name" error={errors.fullName?.message}>
            <Input {...register('fullName')} placeholder="Jan Kowalski" />
          </Field>
          <Field label="Headline" error={errors.headline?.message}>
            <Input {...register('headline')} placeholder="Junior Java Backend Developer" />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Email" error={errors.email?.message}>
              <Input {...register('email')} placeholder="jan@example.com" />
            </Field>
            <Field label="Phone" error={errors.phone?.message}>
              <Input {...register('phone')} placeholder="+48 123 456 789" />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="LinkedIn" error={errors.linkedInUrl?.message}>
              <Input {...register('linkedInUrl')} placeholder="linkedin.com/in/..." />
            </Field>
            <Field label="GitHub" error={errors.githubUrl?.message}>
              <Input {...register('githubUrl')} placeholder="github.com/..." />
            </Field>
          </div>
          <Field label="Summary" error={errors.summary?.message}>
            <Textarea {...register('summary')} rows={4} placeholder="Passionate junior developer..." />
          </Field>
          <div className="w-40">
            <Label>CV language</Label>
            <Controller
              control={control as Control<Values>}
              name="language"
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {LANGUAGES.map((l) => (
                      <SelectItem key={l} value={l}>
                        {l}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
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
