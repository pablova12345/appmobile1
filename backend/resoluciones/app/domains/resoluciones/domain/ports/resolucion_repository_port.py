"""
Puerto (interfaz) del repositorio de resoluciones. La implementacion concreta
(`infrastructure/sql_resolucion_repository.py`) guarda tambien los bytes de cada
pagina; si en el futuro se quiere mover las imagenes a disco / almacenamiento de
objetos, se cambia solo esa implementacion.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Optional

from app.domains.resoluciones.domain.entities.resolucion import EstadoResolucion, Resolucion


class ResolucionRepositoryPort(ABC):
    @abstractmethod
    def crear(
        self, resolucion: Resolucion, paginas: list[tuple[int, str, str, bytes]]
    ) -> Resolucion:
        """`paginas`: lista de (orden, mime, nombre_archivo, bytes)."""

    @abstractmethod
    def listar_por_usuario(self, usuario_sub: str) -> list[Resolucion]:
        """Sin bytes de imagen. Excluye las eliminadas (soft delete)."""

    @abstractmethod
    def obtener(self, id_resolucion: str, usuario_sub: str) -> Optional[Resolucion]:
        """Detalle con metadata de paginas y estado de tabla. Sin bytes."""

    @abstractmethod
    def obtener_pagina(
        self, id_resolucion: str, orden: int, usuario_sub: str
    ) -> Optional[tuple[str, bytes]]:
        """(mime, bytes) de una pagina, o None."""

    @abstractmethod
    def guardar_tabla(
        self,
        id_resolucion: str,
        usuario_sub: str,
        tabla: dict[str, Any],
        estado: EstadoResolucion,
    ) -> Optional[Resolucion]:
        ...

    @abstractmethod
    def eliminar(self, id_resolucion: str, usuario_sub: str) -> bool:
        """Soft delete. True si existia y se marco."""
