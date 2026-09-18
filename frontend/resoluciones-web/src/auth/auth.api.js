import { API } from '@/core/config'
import { httpClient } from '@/core/httpClient'
import { userFromToken } from '@/core/jwt'
import { storage } from '@/core/storage'

export const authApi = {
  async login({ username, password }) {
    const u = username.trim()
    const data = await httpClient.post(API.AUTH.LOGIN, { username: u, password }, { requiresAuth: false })
    if (!data.access_token) throw new Error('El servidor no devolvió un token.')
    storage.setToken(data.access_token)
    if (data.refresh_token) storage.setRefresh(data.refresh_token)
    storage.setUsername(u)
    return userFromToken(data.access_token, u)
  },

  logout() {
    storage.clear()
  },

  currentUser() {
    const token = storage.getToken()
    if (!token) return null
    return userFromToken(token, storage.getUsername())
  },
}
