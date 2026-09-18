# Resoluciones — backend

Backend del módulo **Resoluciones** de la app SisCat. Guarda las resoluciones
escaneadas desde el móvil (páginas + N° de resolución + nombre, por usuario) para
que después se trabajen desde la web. El OCR de la tabla "RELACIÓN DE SUPERFICIE"
y la generación del Excel (Hoja2) se hacen en el **frontend web**, no acá — igual
que el módulo `geoextraccion` del `proyecto-erp`.

Está escrito para integrarse como un dominio del ERP
(`github.com/MateoBazo/proyecto-erp`): mismo stack (FastAPI + PostgreSQL +
SQLAlchemy), misma arquitectura hexagonal (`domain / application /
infrastructure / presentation / contracts`), misma validación de JWT de Keycloak
por JWKS, mismo realm/cliente (`alcaldia-idec` / `app-idec`).

## Cómo correrlo (standalone, para desarrollo)

```bash
cd backend/resoluciones
python -m venv .venv
.venv\Scripts\activate            # Windows (bash: source .venv/Scripts/activate)
pip install -r requirements.txt
copy ..\..\.env.example ..\..\.env   # un solo .env en la raiz del repo, compartido con frontend/resoluciones-web/, completar KEYCLOAK_CLIENT_SECRET
python -m app.main                # http://localhost:8080  (docs: /docs)
```

Por defecto usa **SQLite** (`resoluciones.db`, se crea solo) — no hace falta
instalar Postgres ni Docker. `KEYCLOAK_CLIENT_SECRET` es el mismo valor que
`EXPO_PUBLIC_KEYCLOAK_CLIENT_SECRET` de la app móvil; solo lo usa este backend
contra Keycloak, nunca llega al navegador.

Tests: `python -m unittest discover -s app/domains/resoluciones/tests`

## Endpoints (`/api`)

| Método | Ruta | Quién | Qué |
|---|---|---|---|
| `POST` | `/login`, `/refresh`, `/me` | — | Auth ROPC contra Keycloak (**standalone only**, ver abajo) |
| `POST` | `/resoluciones` | móvil / web | Sube páginas (`multipart`: `paginas[]` + `nro_resolucion` + `nombre`). Estado inicial `pendiente_ocr` |
| `GET` | `/resoluciones` | web / móvil | Lista las del usuario autenticado |
| `GET` | `/resoluciones/{id}` | web | Detalle + URLs de páginas + estado de la tabla |
| `GET` | `/resoluciones/{id}/paginas/{orden}` | web | Bytes de una página (imagen) |
| `PUT` | `/resoluciones/{id}/tabla` | web | Guarda el editor de tabla (`{ tabla, estado? }`). El backend no interpreta `tabla` |
| `DELETE` | `/resoluciones/{id}` | web | Baja lógica |

Todo (menos auth) exige `Authorization: Bearer <access_token de Keycloak>` y
está acotado al usuario dueño (`JWT.sub`).

## Integración al proyecto-erp

1. Copiar `app/domains/resoluciones/` a `backend/app/domains/` del ERP.
2. En `backend/app/registry.py` del ERP, agregar:
   `from app.domains.resoluciones.presentation.router import router as resoluciones_router`
   `api_router.include_router(resoluciones_router)` (el prefijo `/resoluciones`
   ya viene dentro del router, como `geoextraccion`).
3. **Borrar `app/auth/`** de este proyecto: el login/refresh y la dependencia
   `get_current_user` los provee el dominio `seguridad` del ERP. Repuntar en
   `app/domains/resoluciones/presentation/deps.py` el import
   `from app.auth.deps import get_current_user`
   → `from app.domains.seguridad.presentation.deps import get_current_user`
   (la versión del ERP devuelve un `UserProfile`; este módulo solo usa `.sub`).
4. `app/core/` de acá es un subconjunto del `core/` del ERP (config, database,
   security/JWKS, errors) — usar el del ERP, no duplicar.
5. Base de datos: los modelos declaran el schema `resoluciones` cuando
   `DATABASE_URL` es PostgreSQL (`settings.DB_SCHEMA`). Con Alembic (que el ERP
   todavía no usa), generar la migración del schema `resoluciones`.

## Pendiente / notas

- El camino **PostgreSQL** (creación de schema + `search_path`) está escrito
  pero solo probado el de SQLite. Verificar contra un Postgres real antes de
  integrar.
- Sin `auditoria.evento_auditoria` (el ERP lo pide para escrituras de negocio) —
  se agrega al integrar, cuando exista ese contrato.
- Las imágenes se guardan como BLOB en la misma BD. Si se prefiere disco /
  almacenamiento de objetos, se cambia solo
  `infrastructure/sql_resolucion_repository.py` (el puerto no expone que son BLOB).
