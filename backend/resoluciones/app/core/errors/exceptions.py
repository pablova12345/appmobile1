"""
Jerarquia de excepciones de dominio. Mismo contrato que
`proyecto-erp/backend/app/core/errors/exceptions.py`: cada subclase declara
`http_status` (y opcionalmente `headers`) para que el handler generico las
traduzca a HTTP sin conocer el tipo concreto.
"""
from typing import Dict, Optional


class DomainException(Exception):
    http_status: int = 400
    headers: Optional[Dict[str, str]] = None

    def __init__(self, message: str):
        super().__init__(message)
        self.message = message


class NotFoundException(DomainException):
    """El recurso pedido no existe (o no pertenece al usuario)."""

    http_status = 404


class UnauthorizedException(DomainException):
    """Autenticado pero sin permiso para la accion."""

    http_status = 403


class ValidationException(DomainException):
    """Entrada invalida a nivel de negocio (no de schema)."""

    http_status = 422
