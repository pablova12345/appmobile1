import ListaResolucionesPage from '@/domains/resoluciones/pages/ListaResolucionesPage'
import ResolucionPage from '@/domains/resoluciones/pages/ResolucionPage'

/**
 * Rutas del dominio `resoluciones`. Se registran en `src/app/App.jsx` (equivalente
 * frontend de `backend/app/registry.py`). En el proyecto-erp esto va en
 * `Frontend/src/domains/resoluciones/routes.jsx` y se suma al router raíz.
 */
export const resolucionesRoutes = [
  { path: '/resoluciones', element: <ListaResolucionesPage /> },
  { path: '/resoluciones/:id', element: <ResolucionPage /> },
]
