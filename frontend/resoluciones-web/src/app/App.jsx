import { Navigate, Route, Routes } from 'react-router-dom'

import LoginPage from '@/auth/LoginPage'
import { ProtectedRoute } from '@/auth/ProtectedRoute'
import { AppShell } from '@/app/layout/AppShell'
import { resolucionesRoutes } from '@/domains/resoluciones/routes'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route index element={<Navigate to="/resoluciones" replace />} />
          {resolucionesRoutes.map((r) => (
            <Route key={r.path} path={r.path} element={r.element} />
          ))}
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/resoluciones" replace />} />
    </Routes>
  )
}
