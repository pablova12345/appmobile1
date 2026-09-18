"""
Entidades del dominio Resoluciones. Sin FastAPI, SQLAlchemy ni Pydantic.

Una "resolucion" es un conjunto de paginas escaneadas (desde el movil) de una
resolucion administrativa que trae la tabla "RELACION DE SUPERFICIE". El trabajo
sobre esa tabla (OCR + asignar columnas + correcciones + Excel) se hace en el
frontend web; el backend solo guarda las paginas y el estado de la tabla.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Any, Optional


class EstadoResolucion(str, Enum):
    PENDIENTE_OCR = "pendiente_ocr"  # subida desde el movil, sin trabajar
    EN_PROCESO = "en_proceso"  # alguien la abrio en la web y guardo un borrador
    LISTO = "listo"  # tabla revisada, Excel generado


@dataclass
class Pagina:
    orden: int
    mime: str
    nombre_archivo: Optional[str] = None
    id_pagina: Optional[str] = None


@dataclass
class Resolucion:
    nro_resolucion: str
    nombre: str
    usuario_sub: str
    estado: EstadoResolucion = EstadoResolucion.PENDIENTE_OCR
    id_resolucion: Optional[str] = None
    fecha_creacion: Optional[datetime] = None
    fecha_actualizacion: Optional[datetime] = None
    paginas: list[Pagina] = field(default_factory=list)
    # Estado del editor de tabla que guarda la web, tal cual lo manda:
    # { "columnRoles": [...], "paginas": [...], "plantas": {...} }. El backend no
    # lo interpreta -- es un contrato entre el frontend y si mismo.
    tabla: Optional[dict[str, Any]] = None
