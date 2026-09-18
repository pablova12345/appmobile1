from fastapi import APIRouter

from app.domains.resoluciones.presentation.endpoints.resoluciones import (
    router as resoluciones_router,
)

# El prefijo del recurso va aca (no en registry) para que las rutas "de
# coleccion" puedan usar path "" sin que FastAPI se queje de prefijo+path vacios.
router = APIRouter()
router.include_router(resoluciones_router, prefix="/resoluciones")
