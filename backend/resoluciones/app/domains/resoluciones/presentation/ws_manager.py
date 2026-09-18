"""
Registro en memoria de conexiones WebSocket abiertas, por usuario (`sub` del
JWT). Se usa para avisarle al frontend web que la lista de resoluciones
cambio (se subio, edito o borro una desde el celular o desde otra pestaña)
sin que la persona tenga que apretar un boton de "Actualizar".

Alcanza con memoria de proceso (un dict) porque el backend corre como un solo
proceso uvicorn en este modulo (SQLite) -- no hace falta un broker externo
(Redis pub/sub, etc.) para este tamaño de proyecto.
"""
from __future__ import annotations

from fastapi import WebSocket


class ResolucionesWsManager:
    def __init__(self) -> None:
        self._conexiones: dict[str, set[WebSocket]] = {}

    async def conectar(self, usuario_sub: str, ws: WebSocket) -> None:
        await ws.accept()
        self._conexiones.setdefault(usuario_sub, set()).add(ws)

    def desconectar(self, usuario_sub: str, ws: WebSocket) -> None:
        conexiones = self._conexiones.get(usuario_sub)
        if not conexiones:
            return
        conexiones.discard(ws)
        if not conexiones:
            self._conexiones.pop(usuario_sub, None)

    async def avisar_cambio(self, usuario_sub: str) -> None:
        for ws in list(self._conexiones.get(usuario_sub, ())):
            try:
                await ws.send_json({"tipo": "resoluciones_actualizadas"})
            except Exception:
                self.desconectar(usuario_sub, ws)


ws_manager = ResolucionesWsManager()
