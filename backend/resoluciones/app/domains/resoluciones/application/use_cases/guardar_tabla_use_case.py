from __future__ import annotations

from typing import Any, Optional

from app.domains.resoluciones.domain.entities.resolucion import EstadoResolucion, Resolucion
from app.domains.resoluciones.domain.exceptions import ResolucionInvalida, ResolucionNoEncontrada
from app.domains.resoluciones.domain.ports import ResolucionRepositoryPort


class GuardarTablaUseCase:
    """
    Guarda el estado del editor de tabla que arma la web (roles de columna,
    celdas corregidas, plantas). El backend no interpreta el contenido.
    """

    def __init__(self, repository: ResolucionRepositoryPort):
        self._repo = repository

    def execute(
        self,
        *,
        id_resolucion: str,
        usuario_sub: str,
        tabla: dict[str, Any],
        estado: Optional[str] = None,
    ) -> Resolucion:
        if not isinstance(tabla, dict):
            raise ResolucionInvalida("El cuerpo 'tabla' tiene que ser un objeto.")

        if estado is None:
            nuevo_estado = EstadoResolucion.EN_PROCESO
        else:
            try:
                nuevo_estado = EstadoResolucion(estado)
            except ValueError:
                raise ResolucionInvalida(
                    f"Estado '{estado}' invalido "
                    f"(esperado: {', '.join(e.value for e in EstadoResolucion)})."
                )

        actualizada = self._repo.guardar_tabla(id_resolucion, usuario_sub, tabla, nuevo_estado)
        if actualizada is None:
            raise ResolucionNoEncontrada(id_resolucion)
        return actualizada
