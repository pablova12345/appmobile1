# Resoluciones — frontend web

Módulo web para trabajar las resoluciones que se escanean desde la app móvil:
listar, **extraer la tabla "RELACIÓN DE SUPERFICIE" con OCR**, revisar/corregir,
y **generar la Hoja2 del Excel** (`plantilla-ph.xlsm`).

Stack calcado del `proyecto-erp/Frontend` para que se integre como el dominio
`resoluciones`: **Vite + React 19 + React Router 7 + Tailwind v4 + lucide-react +
react-toastify + SheetJS (xlsx)**. Arquitectura por dominio
(`src/domains/resoluciones/{api,components,pages,utils,routes.jsx,permissions.js}`),
cliente HTTP con Bearer + refresh en 401 (`src/core/httpClient.js`), auth por
`POST /api/login` (ROPC vía backend, igual que el ERP y la app móvil).

El **OCR corre en el navegador** (pega directo a `VITE_OCR_API_URL`), igual que
el módulo `geoextraccion` del ERP. El **Excel también** (SheetJS con
`bookVBA:true`, preserva las macros). El backend de este módulo
(`backend/resoluciones`) sólo guarda/lista/entrega páginas.

## Correrlo

```bash
cd frontend/resoluciones-web
npm install
copy ..\..\.env.example ..\..\.env  # un solo .env en la raiz del repo (envDir: '../../' en vite.config.js)
npm run dev                  # http://localhost:5173
```

Necesita el backend (`backend/resoluciones`, puerto 8080) corriendo, y un
usuario de Keycloak (`alcaldia-idec`) para el login.

## Flujo

1. **Login** (usuario/contraseña Keycloak).
2. **/resoluciones** — lista de las resoluciones del usuario (las sube desde la app móvil).
3. **/resoluciones/:id** — páginas escaneadas → **Extraer con OCR** →
   `superciesOcrParser` reconstruye la tabla por posición, **sugiere el rol de
   cada columna** y **detecta la Planta** (fila-sección "PLANTA X PISO" o columna
   "NIVEL"), y **descarta** filas "SUP. TOTAL" y el bloque "RESUMEN GENERAL".
4. El usuario ajusta roles (los `<select>` vienen pre-seleccionados), corrige las
   celdas en rojo (confianza < `VITE_OCR_THRESHOLD`) y la Planta.
5. **Guardar borrador** (`PUT /tabla`, estado `en_proceso`) para retomar después,
   o **Generar Excel** → descarga el `.xlsm` y marca la resolución como `listo`.

## Integración al proyecto-erp

- Copiar `src/domains/resoluciones/` a `Frontend/src/domains/` del ERP y registrar
  `resolucionesRoutes` en su router raíz (`src/app/routes.jsx`).
- `src/core/` y `src/shared/ui/` de acá son un subconjunto mínimo de los del ERP
  (httpClient, storage, config, componentes UI) — usar los del ERP.
- `src/auth/` de acá replica el login para correr standalone — usar el `auth/` del ERP.
- Mover `public/plantilla-ph.xlsm` a donde el ERP sirva estáticos.
- `permissions.js` lista los permisos RBAC que habría que dar de alta en `seguridad.permisos`.

## Notas / pendiente

- **CORS del servicio OCR**: el navegador le pega directo a `ocr.catastrocbba.com`.
  Si ese servicio no manda cabeceras CORS, la llamada falla desde el browser y
  hay que (a) habilitarlo, o (b) agregar un endpoint proxy en el backend. El
  módulo `geoextraccion` del ERP asume que funciona directo.
- Los umbrales de agrupamiento del parser (`rowGapY`/`mergeGapX`/`columnGapX`)
  se calibraron contra las resoluciones de ejemplo; pueden necesitar retoque con
  otras resoluciones/resoluciones de imagen.
- El bundle pesa ~750 kB (mayormente SheetJS). Para producción conviene
  `manualChunks` o cargar `xlsx` con `import()` diferido.
