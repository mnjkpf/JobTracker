import { Link } from 'react-router-dom'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { LoginForm } from '@/features/auth/LoginForm'

export default function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl">Sign in</CardTitle>
          <CardDescription>Welcome back to JobTracker</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <LoginForm />
          <p className="text-center text-sm text-muted-foreground">
            No account?{' '}
            <Link to="/register" className="font-medium text-primary hover:underline">
              Create one
            </Link>
          </p>

          {/* Guest entry point: opening the board with no token starts an
              anonymous session automatically (see ProtectedRoute). */}
          <div className="border-t pt-4">
            <Link
              to="/"
              className="block text-center text-sm font-medium text-muted-foreground hover:text-foreground hover:underline"
            >
              Or try it without an account
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
