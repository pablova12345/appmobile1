/** Decodifica el payload de un JWT (sin verificar la firma -- eso lo hace el backend). */
export function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decodeURIComponent(escape(json)))
  } catch {
    return null
  }
}

export function isExpired(token, skewSeconds = 30) {
  const p = decodeJwt(token)
  if (!p?.exp) return true
  return Date.now() / 1000 >= p.exp - skewSeconds
}

export function userFromToken(token, fallbackUsername) {
  const p = decodeJwt(token) || {}
  return {
    sub: p.sub || null,
    username: p.preferred_username || fallbackUsername || p.sub || 'usuario',
    email: p.email || null,
    roles: p.realm_access?.roles || [],
  }
}
