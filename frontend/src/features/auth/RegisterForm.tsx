import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useUpgradeAccount } from '@/features/account/hooks'
import { isGuestSession } from '@/lib/jwt'
import { useRegister } from './hooks'

const registerSchema = z.object({
  displayName: z.string().min(1, 'Name is required'),
  email: z.string().email('Enter a valid email'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
})

type RegisterValues = z.infer<typeof registerSchema>

export function RegisterForm() {
  // A guest already has an account row on the backend — registering must upgrade
  // it in place, otherwise everything they tracked so far would be orphaned.
  const guest = isGuestSession()
  const registerUser = useRegister()
  const upgradeAccount = useUpgradeAccount()
  const submitAction = guest ? upgradeAccount : registerUser

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { displayName: '', email: '', password: '' },
  })

  return (
    <form
      onSubmit={handleSubmit((values) => submitAction.mutate(values))}
      className="space-y-4"
      noValidate
    >
      {guest && (
        <p className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-900">
          Everything already on your board stays — this just attaches it to a real
          account so it is not deleted.
        </p>
      )}

      <div className="space-y-2">
        <Label htmlFor="displayName">Name</Label>
        <Input id="displayName" autoComplete="name" placeholder="Jan Kowalski" {...register('displayName')} />
        {errors.displayName && (
          <p className="text-sm text-destructive">{errors.displayName.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          {...register('email')}
        />
        {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
      </div>

      <div className="space-y-2">
        <Label htmlFor="password">Password</Label>
        <Input id="password" type="password" autoComplete="new-password" {...register('password')} />
        {errors.password && <p className="text-sm text-destructive">{errors.password.message}</p>}
      </div>

      <Button type="submit" className="w-full" disabled={submitAction.isPending}>
        {submitAction.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
        {guest ? 'Save my board & create account' : 'Create account'}
      </Button>
    </form>
  )
}
