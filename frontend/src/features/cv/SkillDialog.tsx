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
import { useAddSkill, useUpdateSkill } from './hooks'
import type { Skill, SkillCategory, SkillLevel } from './types'

const CATEGORIES = ['LANGUAGE', 'FRAMEWORK', 'DATABASE', 'TOOL', 'SOFT', 'OTHER'] as const
const LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'] as const
const UNSET = '__unset__'

const schema = z.object({
  name: z.string().min(1, 'Required').max(100),
  category: z.string(),
  level: z.string(),
})

type Values = z.infer<typeof schema>

interface Props {
  skill: Skill | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function SkillDialog({ skill, open, onOpenChange }: Props) {
  const isEdit = !!skill
  const add = useAddSkill()
  const update = useUpdateSkill(skill?.id ?? '')
  const mutation = isEdit ? update : add

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    values: {
      name: skill?.name ?? '',
      category: skill?.category ?? UNSET,
      level: skill?.level ?? UNSET,
    },
  })

  const onSubmit = (values: Values) => {
    if (isEdit) {
      update.mutate(
        { level: values.level === UNSET ? undefined : (values.level as SkillLevel) },
        { onSuccess: () => onOpenChange(false) },
      )
    } else {
      add.mutate(
        {
          name: values.name,
          category: values.category === UNSET ? undefined : (values.category as SkillCategory),
          level: values.level === UNSET ? undefined : (values.level as SkillLevel),
        },
        { onSuccess: () => onOpenChange(false) },
      )
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Edit skill' : 'Add skill'}</DialogTitle>
          <DialogDescription>
            {isEdit ? 'Only the proficiency level can be changed here.' : 'Technical or soft skill for your CV.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          {!isEdit && (
            <Field label="Name" error={errors.name?.message}>
              <Input {...register('name')} placeholder="java" />
            </Field>
          )}
          {!isEdit && (
            <Field label="Category">
              <Controller
                control={control as Control<Values>}
                name="category"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={UNSET}>Not set</SelectItem>
                      {CATEGORIES.map((c) => (
                        <SelectItem key={c} value={c}>
                          {c}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
          )}
          <Field label="Level">
            <Controller
              control={control as Control<Values>}
              name="level"
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={UNSET}>Not set</SelectItem>
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

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      {children}
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  )
}
