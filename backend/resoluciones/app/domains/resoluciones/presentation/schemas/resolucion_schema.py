from __future__ import annotations

from datetime import datetime
from typing import Any, Optional

from pydantic import BaseModel, Field

from app.domains.resoluciones.domain.entities.resolucion import Resolucion


class PaginaOut(BaseModel):
    orden: int
    mime: str
    nombre_archivo: Optional[str] = None
    # URL relativa para descargar la imagen (la arma el endpoint).
    url: str


class ResolucionResumenOut(BaseModel):
    id_resolucion: str
    nro_resolucion: str
    nombre: str
    estado: str
    total_paginas: int
    fecha_creacion: Optional[datetime] = None
    fecha_actualizacion: Optional[datetime] = None

    @classmethod
    def from_entity(cls, r: Resolucion) -> "ResolucionResumenOut":
        return cls(
            id_resolucion=r.id_resolucion or "",
            nro_resolucion=r.nro_resolucion,
            nombre=r.nombre,
            estado=r.estado.value,
            total_paginas=len(r.paginas),
            fecha_creacion=r.fecha_creacion,
            fecha_actualizacion=r.fecha_actualizacion,
        )


class ResolucionDetalleOut(ResolucionResumenOut):
    paginas: list[PaginaOut]
    tabla: Optional[dict[str, Any]] = None

    @classmethod
    def from_entity(cls, r: Resolucion, *, base_url: str) -> "ResolucionDetalleOut":
        base = ResolucionResumenOut.from_entity(r)
        return cls(
            **base.model_dump(),
            tabla=r.tabla,
            paginas=[
                PaginaOut(
                    orden=p.orden,
                    mime=p.mime,
                    nombre_archivo=p.nombre_archivo,
                    url=f"{base_url}/{p.orden}",
                )
                for p in r.paginas
            ],
        )


class GuardarTablaRequest(BaseModel):
    tabla: dict[str, Any]
    estado: Optional[str] = Field(
        default=None,
        description="pendiente_ocr | en_proceso | listo. Si se omite, pasa a 'en_proceso'.",
    )
