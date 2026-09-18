import { createContext, useCallback, useContext, useMemo, useState } from 'react'

import { isExpired } from '@/core/jwt'
import { storage } from '@/core/storage'
import { authApi } from '@/auth/auth.api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = storage.getToken()
    return token && !isExpired(token) ? authApi.currentUser() : null
  })

  const login = useCallback(async (credentials) => {
    const u = await authApi.login(credentials)
    setUser(u)
    return u
  }, [])

  const logout = useCallback(() => {
    authApi.logout()
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, isAuthenticated: Boolean(user), login, logout }),
    [user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
