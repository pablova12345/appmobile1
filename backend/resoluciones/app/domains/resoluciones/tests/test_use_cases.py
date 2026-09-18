"""
Tests de los casos de uso contra un repositorio en memoria (sin BD ni FastAPI).
Correr: `python -m pytest` desde backend/resoluciones/ (o `python -m unittest`).
"""
import unittest
from datetime import datetime
from typing import Any, Optional

from app.domains.resoluciones.application.use_cases import (
    CrearResolucionUseCase,
    GuardarTablaUseCase,
    ListarResolucionesUseCase,
    ObtenerResolucionUseCase,
)
from app.domains.resoluciones.domain.entities.resolucion import (
    EstadoResolucion,
    Pagina,
    Resolucion,
)
from app.domains.resoluciones.domain.exceptions import (
    ResolucionInvalida,
    ResolucionNoEncontrada,
)
from app.domains.resoluciones.domain.ports import ResolucionRepositoryPort

PNG = "image/png"


class FakeRepo(ResolucionRepositoryPort):
    def __init__(self):
        self._rows: dict[str, Resolucion] = {}
        self._bytes: dict[tuple[str, int], tuple[str, bytes]] = {}
        self._seq = 0

    def crear(self, resolucion, paginas):
        self._seq += 1
        rid = f"r{self._seq}"
        resolucion.id_resolucion = rid
        resolucion.fecha_creacion = resolucion.fecha_actualizacion = datetime(2026, 9, 7)
        resolucion.paginas = [Pagina(orden=o, mime=m, nombre_archivo=n) for (o, m, n, _) in paginas]
        self._rows[rid] = resolucion
        for (o, m, _n, b) in paginas:
            self._bytes[(rid, o)] = (m, b)
        return resolucion

    def listar_por_usuario(self, usuario_sub):
        return [r for r in self._rows.values() if r.usuario_sub == usuario_sub]

    def obtener(self, id_resolucion, usuario_sub):
        r = self._rows.get(id_resolucion)
        return r if r and r.usuario_sub == usuario_sub else None

    def obtener_pagina(self, id_resolucion, orden, usuario_sub):
        if not self.obtener(id_resolucion, usuario_sub):
            return None
        return self._bytes.get((id_resolucion, orden))

    def guardar_tabla(self, id_resolucion, usuario_sub, tabla: dict[str, Any], estado):
        r = self.obtener(id_resolucion, usuario_sub)
        if not r:
            return None
        r.tabla = tabla
        r.estado = estado
        return r

    def eliminar(self, id_resolucion, usuario_sub):
        r = self.obtener(id_resolucion, usuario_sub)
        if not r:
            return False
        del self._rows[id_resolucion]
        return True


class CrearResolucionTests(unittest.TestCase):
    def setUp(self):
        self.repo = FakeRepo()
        self.crear = CrearResolucionUseCase(self.repo)

    def test_crea_con_paginas_validas(self):
        r = self.crear.execute(
            nro_resolucion="291/2024",
            nombre="Edificio Don Juan",
            usuario_sub="user-1",
            paginas=[(PNG, "p1.png", b"\x89PNG..."), (PNG, "p2.png", b"\x89PNG...")],
        )
        self.assertEqual(r.estado, EstadoResolucion.PENDIENTE_OCR)
        self.assertEqual(len(r.paginas), 2)
        self.assertEqual(r.paginas[0].orden, 1)

    def test_rechaza_sin_nro(self):
        with self.assertRaises(ResolucionInvalida):
            self.crear.execute(nro_resolucion="  ", nombre="x", usuario_sub="u", paginas=[(PNG, "a", b"x")])

    def test_rechaza_sin_paginas(self):
        with self.assertRaises(ResolucionInvalida):
            self.crear.execute(nro_resolucion="1", nombre="x", usuario_sub="u", paginas=[])

    def test_rechaza_mime_no_soportado(self):
        with self.assertRaises(ResolucionInvalida):
            self.crear.execute(
                nro_resolucion="1", nombre="x", usuario_sub="u", paginas=[("application/pdf", "a.pdf", b"%PDF")]
            )


class RestoDeCasosTests(unittest.TestCase):
    def setUp(self):
        self.repo = FakeRepo()
        self.crear = CrearResolucionUseCase(self.repo)
        self.r = self.crear.execute(
            nro_resolucion="1/2026", nombre="X", usuario_sub="u1", paginas=[(PNG, "p", b"data")]
        )

    def test_listar_solo_del_usuario(self):
        self.crear.execute(nro_resolucion="2/2026", nombre="Y", usuario_sub="u2", paginas=[(PNG, "p", b"d")])
        listado = ListarResolucionesUseCase(self.repo).execute(usuario_sub="u1")
        self.assertEqual([x.nro_resolucion for x in listado], ["1/2026"])

    def test_obtener_ajeno_da_not_found(self):
        with self.assertRaises(ResolucionNoEncontrada):
            ObtenerResolucionUseCase(self.repo).execute(id_resolucion=self.r.id_resolucion, usuario_sub="otro")

    def test_guardar_tabla_cambia_estado(self):
        actualizada = GuardarTablaUseCase(self.repo).execute(
            id_resolucion=self.r.id_resolucion,
            usuario_sub="u1",
            tabla={"columnRoles": ["ambiente"], "paginas": []},
        )
        self.assertEqual(actualizada.estado, EstadoResolucion.EN_PROCESO)
        self.assertEqual(actualizada.tabla["columnRoles"], ["ambiente"])

    def test_guardar_tabla_estado_invalido(self):
        with self.assertRaises(ResolucionInvalida):
            GuardarTablaUseCase(self.repo).execute(
                id_resolucion=self.r.id_resolucion, usuario_sub="u1", tabla={}, estado="terminado"
            )


if __name__ == "__main__":
    unittest.main()
