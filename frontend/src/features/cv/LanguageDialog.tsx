import type { ReactNode } from 'react'
import { Controller, useForm, type Control } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
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
import { useAddLanguage, useUpdateLanguage } from './hooks'
import type { Language, LanguageLevel } from './types'

const LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2', 'NATIVE'] as const

const schema = z.object({
  name: z.string().min(1, 'Required').max(100),
  level: z.enum(LEVELS),
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
  language: Language | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function LanguageDialog({ language, open, onOpenChange }: Props) {
  const isEdit = !!language
  const add = useAddLanguage()
  const update = useUpdateLanguage(language?.id ?? '')
  const mutation = isEdit ? update : add

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    values: {
      name: language?.name ?? '',
      level: language?.level ?? 'B1',
    },
  })

  const onSubmit = (values: Values) => {
    mutation.mutate(values, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Edit language' : 'Add language'}</DialogTitle>
          <DialogDescription>A language you speak, shown on your CV.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <Field label="Name" error={errors.name?.message}>
            <Input {...register('name')} placeholder="English" />
          </Field>
          <Field label="Level">
            <Controller
              control={control as Control<Values>}
              name="level"
              render={({ field }) => (
                <Select value={field.value} onValueChange={(v) => field.onChange(v as LanguageLevel)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {LEVELS.map((l) => (
                      <SelectItem key={l} value={l}>
                        {l}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
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
