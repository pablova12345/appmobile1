/** Variables de entorno de la app (Vite las inyecta en build). */
export const ENV = {
  API_BASE_URL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  OCR_API_URL: (import.meta.env.VITE_OCR_API_URL || 'https://ocr.catastrocbba.com').replace(
    /\/+$/,
    '',
  ),
  OCR_THRESHOLD: parseFloat(import.meta.env.VITE_OCR_THRESHOLD || '0.85'),
  APP_NAME: 'Resoluciones',
  ORG: 'IDEC · GAMC',
}

/** Rutas del backend (mismo estilo que endpoints.config.js del proyecto-erp). */
export const API = {
  AUTH: {
    LOGIN: '/api/login',
    REFRESH: '/api/refresh',
    ME: '/api/me',
  },
  RESOLUCIONES: {
    BASE: '/api/resoluciones',
    ONE: (id) => `/api/resoluciones/${id}`,
    PAGINA: (id, orden) => `/api/resoluciones/${id}/paginas/${orden}`,
    TABLA: (id) => `/api/resoluciones/${id}/tabla`,
  },
}

export const STORAGE_KEYS = {
  TOKEN: 'resoluciones.token',
  REFRESH: 'resoluciones.refresh',
  USERNAME: 'resoluciones.username',
}
