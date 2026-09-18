import { STORAGE_KEYS } from '@/core/config'

const get = (k) => {
  try {
    return localStorage.getItem(k)
  } catch {
    return null
  }
}
const set = (k, v) => {
  try {
    if (v == null) localStorage.removeItem(k)
    else localStorage.setItem(k, v)
  } catch {
    /* modo privado / storage bloqueado */
  }
}

export const storage = {
  getToken: () => get(STORAGE_KEYS.TOKEN),
  setToken: (v) => set(STORAGE_KEYS.TOKEN, v),
  getRefresh: () => get(STORAGE_KEYS.REFRESH),
  setRefresh: (v) => set(STORAGE_KEYS.REFRESH, v),
  getUsername: () => get(STORAGE_KEYS.USERNAME),
  setUsername: (v) => set(STORAGE_KEYS.USERNAME, v),
  clear: () => {
    set(STORAGE_KEYS.TOKEN, null)
    set(STORAGE_KEYS.REFRESH, null)
    set(STORAGE_KEYS.USERNAME, null)
  },
}
