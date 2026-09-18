from __future__ import annotations

from app.domains.resoluciones.domain.entities.resolucion import Resolucion
from app.domains.resoluciones.domain.ports import ResolucionRepositoryPort


class ListarResolucionesUseCase:
    def __init__(self, repository: ResolucionRepositoryPort):
        self._repo = repository

    def execute(self, *, usuario_sub: str) -> list[Resolucion]:
        return self._repo.listar_por_usuario(usuario_sub)
