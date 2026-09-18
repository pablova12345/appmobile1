from __future__ import annotations

from app.domains.resoluciones.domain.entities.resolucion import (
    EstadoResolucion,
    Pagina,
    Resolucion,
)
from app.domains.resoluciones.domain.exceptions import ResolucionInvalida
from app.domains.resoluciones.domain.ports import ResolucionRepositoryPort

MIME_PERMITIDOS = {"image/jpeg", "image/png", "image/webp"}
MAX_PAGINAS = 40


class CrearResolucionUseCase:
    def __init__(self, repository: ResolucionRepositoryPort):
        self._repo = repository

    def execute(
        self,
        *,
        nro_resolucion: str,
        nombre: str,
        usuario_sub: str,
        paginas: list[tuple[str, str, bytes]],  # (mime, nombre_archivo, bytes)
    ) -> Resolucion:
        nro = (nro_resolucion or "").strip()
        nom = (nombre or "").strip()
        if not nro:
            raise ResolucionInvalida("El numero de resolucion es obligatorio.")
        if not nom:
            raise ResolucionInvalida("El nombre es obligatorio.")
        if not paginas:
            raise ResolucionInvalida("Hay que subir al menos una pagina escaneada.")
        if len(paginas) > MAX_PAGINAS:
            raise ResolucionInvalida(f"Demasiadas paginas (maximo {MAX_PAGINAS}).")

        para_guardar: list[tuple[int, str, str, bytes]] = []
        for i, (mime, nombre_archivo, contenido) in enumerate(paginas, start=1):
            mime = (mime or "").lower()
            if mime not in MIME_PERMITIDOS:
                raise ResolucionInvalida(
                    f"Pagina {i}: formato '{mime}' no soportado (usar JPEG, PNG o WebP)."
                )
            if not contenido:
                raise ResolucionInvalida(f"Pagina {i}: archivo vacio.")
            para_guardar.append((i, mime, nombre_archivo or f"pagina_{i}", contenido))

        entidad = Resolucion(
            nro_resolucion=nro,
            nombre=nom,
            usuario_sub=usuario_sub,
            estado=EstadoResolucion.PENDIENTE_OCR,
            paginas=[Pagina(orden=o, mime=m, nombre_archivo=n) for (o, m, n, _) in para_guardar],
        )
        return self._repo.crear(entidad, para_guardar)
