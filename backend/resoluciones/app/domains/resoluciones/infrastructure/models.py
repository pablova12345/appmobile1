"""
Modelos ORM del dominio Resoluciones.

Los atributos Python siguen la convencion del proyecto-erp (CLAUDE.md seccion
6: snake_case, PK `id_<entidad>`, `fecha_creacion`/`fecha_actualizacion`,
soft delete `fecha_eliminacion`), pero cada columna se mapea explicitamente
(`mapped_column("nombre_real_en_bd", ...)`) al nombre real de la tabla
`resolutions`/`resolution_pages` que ya existe en Postgres (`idec_erp`,
schema `settings.DB_SCHEMA` = "resolutions", creada aparte con nombres en
ingles -- no las crea este backend). Esto evita tocar el resto del codigo
(use cases, repositorio, tests), que solo conoce los atributos en espaniol.
En SQLite (dev sin Postgres) no hay schema, pero las columnas fisicas siguen
siendo las mismas (incluso ahi se llaman igual que en Postgres).

Las imagenes de las paginas se guardan como BLOB en la misma BD (columna
`image`). Si se quisiera moverlas a disco / almacenamiento de objetos, se
cambia solo `sql_resolucion_repository.py` (la interfaz del puerto no expone que
son BLOB).
"""
from __future__ import annotations

import uuid

from sqlalchemy import (
    JSON,
    DateTime,
    ForeignKey,
    Integer,
    LargeBinary,
    String,
    Text,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.config import settings
from app.core.database.connection import Base

_SCHEMA = None if settings.is_sqlite else settings.DB_SCHEMA


def _table_args() -> dict:
    return {} if _SCHEMA is None else {"schema": _SCHEMA}


def _fk(target: str) -> str:
    # "resolutions.resolution_id" -> "resolutions.resolutions.resolution_id" en Postgres
    return target if _SCHEMA is None else f"{_SCHEMA}.{target}"


def _uuid() -> str:
    return str(uuid.uuid4())


class ResolucionModel(Base):
    __tablename__ = "resolutions"
    __table_args__ = _table_args()

    id_resolucion: Mapped[str] = mapped_column("resolution_id", String(36), primary_key=True, default=_uuid)
    nro_resolucion: Mapped[str] = mapped_column("resolution_number", String(120), nullable=False, index=True)
    nombre: Mapped[str] = mapped_column("name", String(255), nullable=False)
    estado: Mapped[str] = mapped_column("status", String(20), nullable=False, default="pendiente_ocr", index=True)
    usuario_sub: Mapped[str] = mapped_column("user_sub", String(64), nullable=False, index=True)

    # Estado del editor de tabla de la web (opaco para el backend).
    tabla: Mapped[dict | None] = mapped_column("table_data", JSON(none_as_null=True), nullable=True)

    fecha_creacion: Mapped["DateTime"] = mapped_column(
        "created_at", DateTime, server_default=func.now(), nullable=False
    )
    fecha_actualizacion: Mapped["DateTime"] = mapped_column(
        "updated_at", DateTime, server_default=func.now(), onupdate=func.now(), nullable=False
    )
    fecha_eliminacion: Mapped["DateTime | None"] = mapped_column("deleted_at", DateTime, nullable=True, index=True)

    paginas: Mapped[list["PaginaModel"]] = relationship(
        back_populates="resolucion",
        cascade="all, delete-orphan",
        order_by="PaginaModel.orden",
    )


class PaginaModel(Base):
    __tablename__ = "resolution_pages"
    __table_args__ = _table_args()

    id_pagina: Mapped[str] = mapped_column("page_id", String(36), primary_key=True, default=_uuid)
    id_resolucion: Mapped[str] = mapped_column(
        "resolution_id",
        String(36),
        ForeignKey(_fk("resolutions.resolution_id"), ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    orden: Mapped[int] = mapped_column("order_index", Integer, nullable=False)
    mime: Mapped[str] = mapped_column(String(40), nullable=False)
    nombre_archivo: Mapped[str | None] = mapped_column("file_name", Text, nullable=True)
    imagen: Mapped[bytes] = mapped_column("image", LargeBinary, nullable=False)

    fecha_creacion: Mapped["DateTime"] = mapped_column(
        "created_at", DateTime, server_default=func.now(), nullable=False
    )

    resolucion: Mapped["ResolucionModel"] = relationship(back_populates="paginas")
