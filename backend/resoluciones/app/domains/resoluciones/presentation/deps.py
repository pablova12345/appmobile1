"""
Inyeccion de dependencias del dominio Resoluciones: sesion de BD -> repositorio
-> casos de uso. `get_current_user` se reexporta desde `app.auth.deps` (en el
ERP vendria de `domains/seguridad/presentation/deps.py`).
"""
from fastapi import Depends
from sqlalchemy.orm import Session

from app.auth.deps import get_current_user  # noqa: F401  (reexport)
from app.core.database.connection import get_db
from app.domains.resoluciones.application.use_cases import (
    CrearResolucionUseCase,
    EliminarResolucionUseCase,
    GuardarTablaUseCase,
    ListarResolucionesUseCase,
    ObtenerResolucionUseCase,
)
from app.domains.resoluciones.domain.ports import ResolucionRepositoryPort
from app.domains.resoluciones.infrastructure.sql_resolucion_repository import (
    SqlResolucionRepository,
)


def get_repository(db: Session = Depends(get_db)) -> ResolucionRepositoryPort:
    return SqlResolucionRepository(db=db)


def get_crear_resolucion_use_case(
    repo: ResolucionRepositoryPort = Depends(get_repository),
) -> CrearResolucionUseCase:
    return CrearResolucionUseCase(repository=repo)


def get_listar_resoluciones_use_case(
    repo: ResolucionRepositoryPort = Depends(get_repository),
) -> ListarResolucionesUseCase:
    return ListarResolucionesUseCase(repository=repo)


def get_obtener_resolucion_use_case(
    repo: ResolucionRepositoryPort = Depends(get_repository),
) -> ObtenerResolucionUseCase:
    return ObtenerResolucionUseCase(repository=repo)


def get_guardar_tabla_use_case(
    repo: ResolucionRepositoryPort = Depends(get_repository),
) -> GuardarTablaUseCase:
    return GuardarTablaUseCase(repository=repo)


def get_eliminar_resolucion_use_case(
    repo: ResolucionRepositoryPort = Depends(get_repository),
) -> EliminarResolucionUseCase:
    return EliminarResolucionUseCase(repository=repo)
