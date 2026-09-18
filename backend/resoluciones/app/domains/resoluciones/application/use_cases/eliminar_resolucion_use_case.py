from __future__ import annotations

from app.domains.resoluciones.domain.exceptions import ResolucionNoEncontrada
from app.domains.resoluciones.domain.ports import ResolucionRepositoryPort


class EliminarResolucionUseCase:
    def __init__(self, repository: ResolucionRepositoryPort):
        self._repo = repository

    def execute(self, *, id_resolucion: str, usuario_sub: str) -> None:
        if not self._repo.eliminar(id_resolucion, usuario_sub):
            raise ResolucionNoEncontrada(id_resolucion)
