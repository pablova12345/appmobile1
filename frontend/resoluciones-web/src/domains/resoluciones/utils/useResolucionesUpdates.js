import { useEffect, useRef } from 'react'

import { API, ENV } from '@/core/config'
import { storage } from '@/core/storage'

const RETRY_MS = 3000

function wsUrl() {
  const token = storage.getToken()
  if (!token) return null
  const base = ENV.API_BASE_URL.replace(/^http/, 'ws')
  return `${base}${API.RESOLUCIONES.BASE}/ws?token=${encodeURIComponent(token)}`
}

/**
 * Conecta al websocket de Resoluciones y llama `onUpdate` cada vez que el
 * backend avisa un cambio (alguien subio/edito/borro una resolucion, desde el
 * celular o desde otra pestaña) -- reemplaza al boton manual de "Actualizar".
 *
 * Reconecta con un delay fijo ante cualquier corte (token vencido, backend
 * reiniciado, red): no hace falta backoff exponencial para esta escala.
 */
export function useResolucionesUpdates(onUpdate) {
  const onUpdateRef = useRef(onUpdate)
  onUpdateRef.current = onUpdate

  useEffect(() => {
    let ws
    let cerrado = false
    let reintentoTimer

    const conectar = () => {
      const url = wsUrl()
      if (!url) return
      ws = new WebSocket(url)
      ws.onmessage = () => onUpdateRef.current()
      ws.onclose = () => {
        if (cerrado) return
        reintentoTimer = setTimeout(conectar, RETRY_MS)
      }
      ws.onerror = () => ws.close()
    }

    conectar()

    return () => {
      cerrado = true
      clearTimeout(reintentoTimer)
      ws?.close()
    }
  }, [])
}
