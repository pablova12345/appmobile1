import { FileText, LogOut, User } from 'lucide-react'
import { Link, Outlet, useNavigate } from 'react-router-dom'

import { ENV } from '@/core/config'
import { useAuth } from '@/auth/AuthContext'
import { GisBackdrop } from '@/shared/layout/GisBackdrop'
import { Button } from '@/shared/ui'

export function AppShell() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="relative flex min-h-dvh flex-col text-slate-800 antialiased">
      <GisBackdrop />

      {/* barra superior fina (guiño al header del proyecto-erp) */}
      <div className="h-1.5 w-full bg-brand-900" />

      <header className="sticky top-0 z-10 border-b border-accent-400/40 bg-accent-300/95 px-4 py-3 shadow-sm">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4">
          <Link
            to="/resoluciones"
            className="flex items-center gap-2 text-brand-800 transition-transform duration-200 hover:scale-[1.02]"
          >
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-white/70 text-accent-600 ring-1 ring-white/60">
              <FileText className="h-5 w-5" />
            </span>
            <span className="text-sm font-bold sm:text-base">{ENV.APP_NAME}</span>
            <span className="hidden text-xs text-brand-600/70 sm:inline">· {ENV.ORG}</span>
          </Link>

          <div className="flex items-center gap-2">
            {user && (
              <span className="hidden items-center gap-1.5 text-sm font-medium text-brand-600 sm:flex">
                <User className="h-4 w-4" />
                {user.username}
              </span>
            )}
            <Button
              variant="danger"
              size="sm"
              icon={LogOut}
              onClick={() => {
                logout()
                navigate('/login', { replace: true })
              }}
            >
              Cerrar sesión
            </Button>
          </div>
        </div>
      </header>

      <main className="flex-1 overflow-y-auto px-4 py-8 sm:px-8">
        <div className="mx-auto max-w-5xl space-y-6">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

export default AppShell
