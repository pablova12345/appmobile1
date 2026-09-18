"""
Unico lugar que conoce los routers. Mismo patron que
`proyecto-erp/backend/app/registry.py` (si un dominio falla al importar, se
omite con una advertencia en vez de tumbar el arranque).

Al integrar al ERP: el router de `app.auth` se descarta (lo cubre el dominio
`seguridad`), y el router de `resoluciones` se agrega al `api_router` del ERP
igual que aca (el prefijo `/resoluciones` ya viene dentro de su propio router,
como en el dominio `geoextraccion`).
"""
import logging

from fastapi import APIRouter

logger = logging.getLogger("uvicorn.error")

api_router = APIRouter()

try:
    from app.auth.router import router as auth_router

    api_router.include_router(auth_router)
except Exception as exc:  # pragma: no cover
    logger.warning(f"No se pudo cargar 'auth': {exc}")

try:
    from app.domains.resoluciones.presentation.router import router as resoluciones_router

    api_router.include_router(resoluciones_router)
except Exception as exc:  # pragma: no cover
    logger.warning(f"No se pudo cargar el dominio 'resoluciones': {exc}")
