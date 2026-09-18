"""
Traduce las excepciones de dominio a respuestas HTTP JSON `{ "detail": "..." }`
(mismo formato que espera el `httpClient` del frontend del ERP).
"""
import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.core.errors.exceptions import DomainException
from app.core.security.exceptions import SecurityException

logger = logging.getLogger("uvicorn.error")


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(DomainException)
    async def _domain_exc_handler(request: Request, exc: DomainException):
        return JSONResponse(
            status_code=exc.http_status,
            content={"detail": exc.message},
            headers=exc.headers or None,
        )

    @app.exception_handler(SecurityException)
    async def _security_exc_handler(request: Request, exc: SecurityException):
        logger.warning(
            "SecurityException on %s %s: [%s] %s",
            request.method,
            request.url.path,
            type(exc).__name__,
            exc.message,
        )
        return JSONResponse(
            status_code=exc.http_status,
            content={"detail": exc.message},
            headers=exc.headers or None,
        )

    @app.exception_handler(Exception)
    async def _unhandled_exc_handler(request: Request, exc: Exception):
        logger.exception("Error no controlado en %s %s", request.method, request.url.path)
        return JSONResponse(
            status_code=500,
            content={"detail": "Error interno del servidor."},
        )
