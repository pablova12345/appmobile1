import { useState } from 'react'
import { FileText, KeyRound, User } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'

import { ENV } from '@/core/config'
import { useAuth } from '@/auth/AuthContext'
import { GisBackdrop } from '@/shared/layout/GisBackdrop'
import { Alert, Button, Input } from '@/shared/ui'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const from = location.state?.from?.pathname || '/resoluciones'

  const onSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login({ username, password })
      navigate(from, { replace: true })
    } catch (err) {
      setError(err.message || 'No se pudo iniciar sesión.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="relative flex min-h-dvh items-center justify-center px-4 py-12">
      <GisBackdrop />
      <div className="w-full max-w-sm animate-card-in">
        <div className="rounded-3xl border border-white/50 bg-white/55 p-8 shadow-[0_8px_32px_rgba(100,116,139,0.2),inset_0_1px_1px_rgba(255,255,255,0.6)] backdrop-blur-md sm:p-9">
          <div className="mb-7 flex flex-col items-center text-center">
            <span className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-accent-50 text-accent-600 ring-1 ring-accent-200">
              <FileText className="h-7 w-7" />
            </span>
            <p className="mb-1 text-xs font-semibold uppercase tracking-widest text-accent-600">
              {ENV.ORG}
            </p>
            <h1 className="text-xl font-bold text-slate-900">{ENV.APP_NAME}</h1>
          </div>

          <form className="flex flex-col gap-4" onSubmit={onSubmit}>
            <Input
              id="username"
              label="Usuario"
              icon={User}
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
            <Input
              id="password"
              label="Contraseña"
              type="password"
              icon={KeyRound}
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <Alert>{error}</Alert>
            <Button type="submit" size="lg" loading={loading} className="mt-1 w-full">
              Ingresar
            </Button>
          </form>
        </div>
      </div>
    </main>
  )
}
