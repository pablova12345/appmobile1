# DigiD — Digitalización de Trámites SisCat

App móvil (React Native + **Expo SDK 54**, managed) para escanear, adjuntar y enviar
documentos de trámites del sistema **SisCat** de la Dirección de Administración
Geográfica y Catastro del Gobierno Autónomo Municipal de Cochabamba.

Repo organizado por **frontend/** (clientes) y **backend/** (servicios):

```
frontend/
├── mobile/             App Expo (la app en sí — ver frontend/mobile/README-repo.md
│                        y frontend/mobile/MIGRACION-EXPO.md)
└── resoluciones-web/    Web (Vite+React) para OCR/Excel de resoluciones

backend/
├── server/              Backend "Estudiar con la IA" (Node/Express)
└── resoluciones/        Backend del módulo Resoluciones (FastAPI, standalone)
```

## Arranque rápido (todo el proyecto, un solo comando)

```bash
npm run dev
```

Desde la raíz del repo. La primera vez instala lo que falte (crea el `.venv`
de `backend/resoluciones` e instala sus deps con pip, corre `npm install` en
los proyectos Node que no tengan `node_modules`, y copia los `.env.example` a
`.env` donde falten) y después abre **una ventana de terminal por servicio**:

| Servicio | Ventana | URL |
|---|---|---|
| `backend/resoluciones` (FastAPI) | Resoluciones API | http://localhost:8080/docs |
| `backend/server` (Node, Estudiar con la IA) | Estudiar con IA | http://localhost:4000 |
| `frontend/resoluciones-web` (Vite) | Resoluciones Web | http://localhost:5173 |

Cada servicio va en su propia ventana (no todas mezcladas en una sola consola)
para no mezclar logs y poder cerrar/Ctrl+C cada una por separado.

Antes de correrlo, completar `KEYCLOAK_CLIENT_SECRET` en el `.env` de la raíz
(se crea solo desde `.env.example` la primera vez). **Ollama no lo levanta
este comando** — si `AI_PROVIDER=ollama` en `backend/server/.env`, arrancarlo
aparte (`ollama serve`) antes de `npm run dev`.

Solo `npm run setup` corre la instalación sin abrir las ventanas.

Este comando no levanta la **app móvil** (`frontend/mobile`, Expo) — se corre
aparte con `cd frontend/mobile && npm start` (ver más abajo), porque necesita
escanear un QR desde el celular.

## Arranque rápido (app móvil)

```bash
cd frontend/mobile
npm install
npm start          # abre Metro + QR
```

Escanea el QR con **Expo Go** (el teléfono y la PC deben estar en la misma red
Wi-Fi). Si la red bloquea la conexión directa:

```bash
npx expo start --tunnel
```

## Configuración del backend (app móvil)

La URL del backend y los tipos de archivo permitidos se definen en
`frontend/mobile/app.json → expo.extra` y se leen desde
`frontend/mobile/src/config/env.js`. Para apuntar a otro servidor sin tocar
`app.json`, copiar `frontend/mobile/.env.example` a `frontend/mobile/.env.local`
(ver ese archivo para el detalle de cada variable — backend clásico, IA,
Resoluciones y el ERP real).

## Módulo "Estudiar con la IA" (`backend/server/`)

Backend Node aparte en [`backend/server/`](./backend/server) (ver
[`backend/server/README.md`](./backend/server/README.md)). La app se conecta vía
`EXPO_PUBLIC_AI_BACKEND_URL`. `AIStudyScreen` (se abre desde un documento ya
escaneado) clasifica el tipo de documento y muestra preguntas preescritas, con
Ollama `qwen3-vl:2b` (`AI_PROVIDER` en `backend/server/.env`). Sin el backend
corriendo, esa pantalla no clasifica ni responde. Solo móvil.

## Módulo "Resoluciones"

Para digitalizar la tabla "RELACIÓN DE SUPERFICIE" de las resoluciones y volcarla
a la **Hoja2** de `plantilla-ph.xlsm`. Pensado para entregarse como módulo del
ERP `proyecto-erp` (FastAPI + PostgreSQL + React).

| | Dónde | Qué hace |
|---|---|---|
| **App móvil** (`frontend/mobile/src/screens/home/ResolucionesScreen.jsx`) | Expo | Escanea las páginas con la cámara + N° de resolución + nombre → **sube** al backend. Lista "mis resoluciones" con su estado. No hace OCR ni Excel. |
| **`backend/resoluciones/`** | FastAPI :8080 | Guarda las páginas (BLOB) y el estado de la tabla, por usuario (`JWT.sub`). No hace OCR ni interpreta la tabla. |
| **`frontend/resoluciones-web/`** | Vite :5173 | Lista → abre una resolución → **OCR en el navegador** (`ocr.catastrocbba.com`) → reconstruye la tabla + sugiere roles de columna + detecta Planta → el usuario corrige → **genera el `.xlsm`** (SheetJS, preserva macros). |

`backend/resoluciones` y `frontend/resoluciones-web` comparten **un solo
`.env`** en la raíz del repo (ver [`.env.example`](./.env.example) — `backend/resoluciones`
lo resuelve como ruta absoluta desde su propio archivo, y `frontend/resoluciones-web`
apunta ahí con `envDir` en su `vite.config.js`).

### Levantar todo (dev)

```bash
# 0) config: un solo .env para backend/resoluciones y frontend/resoluciones-web
copy .env.example .env      # y completar KEYCLOAK_CLIENT_SECRET

# 1) backend
cd backend/resoluciones && python -m app.main          # :8080

# 2) frontend
cd frontend/resoluciones-web && npm run dev             # :5173

# 3) app móvil: EXPO_PUBLIC_ERP_BACKEND_URL=http://<IP-PC>:8080 en frontend/mobile/.env.local
```

Los tres validan el mismo token de Keycloak (`alcaldia-idec` / `app-idec`).

Detalle en `backend/resoluciones/README.md` y `frontend/resoluciones-web/README.md`,
incluida la receta de integración al `proyecto-erp`.

## Módulo "Geoextract" (dominio `geoextraction` del ERP real)

La pantalla `GeoextractScreen` de la app móvil sube la foto directo al **ERP
real** (`proyecto_completo/idec-erp-back`, otro repo — no vive acá), dominio
`geoextraction`, vía `EXPO_PUBLIC_IDEC_ERP_BACKEND_URL`. El recorte del área de
coordenadas y el OCR se hacen desde la web del ERP (`idec-erp-front`).

## Estructura (app móvil, `frontend/mobile/`)

```
App.js / index.js         entrada (providers + NavigationContainer)
src/
├── api/                   baseUrl
├── config/                env (expo-constants)
├── navigation/            stack de React Navigation
├── screens/               Login · Home · PDFViewer · AIStudy · Resoluciones · Geoextract
├── components/            modales, listas, botones
├── hooks/                 AuthProvider, WifiLostProvider
├── services/               fileServices, scanService, authService, aiDocService,
│                            resolucionesService, geoextraccionService
└── utils/                 pdf, resize, biometría, credenciales seguras
```

## Limitaciones en Expo Go

- Escáner sin recorte automático de bordes (usa la cámara del sistema).
- En Android el PDF se abre con una app externa (Drive / visor de PDF).

Detalle y alternativa con EAS Build en
[`frontend/mobile/MIGRACION-EXPO.md`](./frontend/mobile/MIGRACION-EXPO.md).

## Flujo de trabajo

Trabaja en tu rama → commit y push → abre un Pull Request para revisión.
