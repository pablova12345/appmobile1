from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Optional

from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.domains.resoluciones.domain.entities.resolucion import (
    EstadoResolucion,
    Pagina,
    Resolucion,
)
from app.domains.resoluciones.domain.ports import ResolucionRepositoryPort
from app.domains.resoluciones.infrastructure.models import PaginaModel, ResolucionModel


def _to_entity(row: ResolucionModel, *, con_paginas: bool = True) -> Resolucion:
    return Resolucion(
        id_resolucion=row.id_resolucion,
        nro_resolucion=row.nro_resolucion,
        nombre=row.nombre,
        usuario_sub=row.usuario_sub,
        estado=EstadoResolucion(row.estado),
        fecha_creacion=row.fecha_creacion,
        fecha_actualizacion=row.fecha_actualizacion,
        tabla=row.tabla,
        paginas=[
            Pagina(orden=p.orden, mime=p.mime, nombre_archivo=p.nombre_archivo, id_pagina=p.id_pagina)
            for p in (row.paginas if con_paginas else [])
        ],
    )


class SqlResolucionRepository(ResolucionRepositoryPort):
    def __init__(self, db: Session):
        self._db = db

    def crear(
        self, resolucion: Resolucion, paginas: list[tuple[int, str, str, bytes]]
    ) -> Resolucion:
        row = ResolucionModel(
            nro_resolucion=resolucion.nro_resolucion,
            nombre=resolucion.nombre,
            estado=resolucion.estado.value,
            usuario_sub=resolucion.usuario_sub,
            tabla=None,
            paginas=[
                PaginaModel(orden=o, mime=m, nombre_archivo=n, imagen=b)
                for (o, m, n, b) in paginas
            ],
        )
        self._db.add(row)
        self._db.commit()
        self._db.refresh(row)
        return _to_entity(row)

    def listar_por_usuario(self, usuario_sub: str) -> list[Resolucion]:
        stmt = (
            select(ResolucionModel)
            .options(selectinload(ResolucionModel.paginas))
            .where(
                ResolucionModel.usuario_sub == usuario_sub,
                ResolucionModel.fecha_eliminacion.is_(None),
            )
            .order_by(ResolucionModel.fecha_creacion.desc())
        )
        return [_to_entity(r) for r in self._db.execute(stmt).scalars().all()]

    def _get_row(self, id_resolucion: str, usuario_sub: str) -> Optional[ResolucionModel]:
        stmt = (
            select(ResolucionModel)
            .options(selectinload(ResolucionModel.paginas))
            .where(
                ResolucionModel.id_resolucion == id_resolucion,
                ResolucionModel.usuario_sub == usuario_sub,
                ResolucionModel.fecha_eliminacion.is_(None),
            )
        )
        return self._db.execute(stmt).scalars().first()

    def obtener(self, id_resolucion: str, usuario_sub: str) -> Optional[Resolucion]:
        row = self._get_row(id_resolucion, usuario_sub)
        return _to_entity(row) if row else None

    def obtener_pagina(
        self, id_resolucion: str, orden: int, usuario_sub: str
    ) -> Optional[tuple[str, bytes]]:
        stmt = (
            select(PaginaModel.mime, PaginaModel.imagen)
            .join(ResolucionModel, ResolucionModel.id_resolucion == PaginaModel.id_resolucion)
            .where(
                PaginaModel.id_resolucion == id_resolucion,
                PaginaModel.orden == orden,
                ResolucionModel.usuario_sub == usuario_sub,
                ResolucionModel.fecha_eliminacion.is_(None),
            )
        )
        res = self._db.execute(stmt).first()
        return (res[0], res[1]) if res else None

    def guardar_tabla(
        self,
        id_resolucion: str,
        usuario_sub: str,
        tabla: dict[str, Any],
        estado: EstadoResolucion,
    ) -> Optional[Resolucion]:
        row = self._get_row(id_resolucion, usuario_sub)
        if row is None:
            return None
        row.tabla = tabla
        row.estado = estado.value
        self._db.commit()
        self._db.refresh(row)
        return _to_entity(row)

    def eliminar(self, id_resolucion: str, usuario_sub: str) -> bool:
        row = self._get_row(id_resolucion, usuario_sub)
        if row is None:
            return False
        row.fecha_eliminacion = datetime.now(timezone.utc)
        self._db.commit()
        return True
