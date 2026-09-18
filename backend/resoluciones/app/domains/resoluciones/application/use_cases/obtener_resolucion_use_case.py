from __future__ import annotations

from app.domains.resoluciones.domain.entities.resolucion import Resolucion
from app.domains.resoluciones.domain.exceptions import ResolucionNoEncontrada
from app.domains.resoluciones.domain.ports import ResolucionRepositoryPort


class ObtenerResolucionUseCase:
    def __init__(self, repository: ResolucionRepositoryPort):
        self._repo = repository

    def execute(self, *, id_resolucion: str, usuario_sub: str) -> Resolucion:
        resolucion = self._repo.obtener(id_resolucion, usuario_sub)
        if resolucion is None:
            raise ResolucionNoEncontrada(id_resolucion)
        return resolucion

    def pagina(
        self, *, id_resolucion: str, orden: int, usuario_sub: str
    ) -> tuple[str, bytes]:
        data = self._repo.obtener_pagina(id_resolucion, orden, usuario_sub)
        if data is None:
            raise ResolucionNoEncontrada(id_resolucion)
        return data
