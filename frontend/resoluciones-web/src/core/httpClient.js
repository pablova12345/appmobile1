import { API, ENV } from '@/core/config'
import { storage } from '@/core/storage'

/**
 * Cliente HTTP base. Port reducido del `httpClient.js` del proyecto-erp:
 * inyecta el Bearer, y ante un 401 intenta UNA renovacion con el refresh_token
 * y reintenta la peticion. Si el refresh falla, limpia la sesion y vuelve a "/".
 */
export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

let refreshPromise = null

async function doRefresh() {
  const refresh_token = storage.getRefresh()
  if (!refresh_token) throw new Error('sin refresh_token')
  const res = await fetch(`${ENV.API_BASE_URL}${API.AUTH.REFRESH}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refresh_token }),
  })
  if (!res.ok) throw new Error('refresh fallido')
  const data = await res.json()
  if (!data.access_token) throw new Error('refresh sin access_token')
  storage.setToken(data.access_token)
  if (data.refresh_token) storage.setRefresh(data.refresh_token)
  return data.access_token
}

function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

function onUnauthorized() {
  storage.clear()
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

async function request(endpoint, { requiresAuth = true, token, headers = {}, body, responseType, _retry = false, ...rest } = {}) {
  const url = endpoint.startsWith('http') ? endpoint : `${ENV.API_BASE_URL}${endpoint}`
  const h = new Headers(headers)
  const isForm = body instanceof FormData
  if (body != null && !isForm && !h.has('Content-Type')) h.set('Content-Type', 'application/json')
  if (requiresAuth) {
    const t = token || storage.getToken()
    if (t) h.set('Authorization', `Bearer ${t}`)
  }

  let res
  try {
    res = await fetch(url, {
      ...rest,
      headers: h,
      body: body == null ? undefined : isForm || typeof body === 'string' ? body : JSON.stringify(body),
    })
  } catch (netErr) {
    throw new ApiError('No se pudo conectar con el servidor. Verificá que esté corriendo.', null, netErr)
  }

  if (res.status === 401 && requiresAuth && !_retry) {
    try {
      const newToken = await refreshAccessToken()
      return request(endpoint, { requiresAuth, token: newToken, headers, body, responseType, _retry: true, ...rest })
    } catch {
      onUnauthorized()
      throw new ApiError('Tu sesión expiró. Volvé a iniciar sesión.', 401)
    }
  }

  if (responseType === 'blob') {
    if (!res.ok) throw new ApiError(`Error ${res.status}`, res.status)
    return res.blob()
  }

  const ct = res.headers.get('content-type') || ''
  const data = ct.includes('application/json') ? await res.json().catch(() => null) : await res.text().catch(() => null)

  if (!res.ok) {
    const msg =
      (data && typeof data === 'object' && (data.detail || data.message)) ||
      (typeof data === 'string' && data) ||
      `Error ${res.status}`
    throw new ApiError(msg, res.status, data)
  }
  return data
}

export const httpClient = {
  get: (e, o) => request(e, { ...o, method: 'GET' }),
  post: (e, body, o) => request(e, { ...o, method: 'POST', body }),
  put: (e, body, o) => request(e, { ...o, method: 'PUT', body }),
  delete: (e, o) => request(e, { ...o, method: 'DELETE' }),
  request,
}
