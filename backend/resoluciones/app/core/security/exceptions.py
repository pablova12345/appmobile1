"""
Excepciones de seguridad. Mismos nombres y http_status que
`proyecto-erp/backend/app/core/security/exceptions.py`.
"""
from typing import Dict, Optional


class SecurityException(Exception):
    http_status: int = 401
    headers: Optional[Dict[str, str]] = None

    def __init__(self, message: str):
        super().__init__(message)
        self.message = message


class InvalidCredentialsException(SecurityException):
    http_status = 401
    headers = {"WWW-Authenticate": "Bearer"}


class TokenVerificationException(SecurityException):
    http_status = 401
    headers = {"WWW-Authenticate": 'Bearer error="invalid_token"'}


class TokenExpiredException(TokenVerificationException):
    headers = {
        "WWW-Authenticate": 'Bearer error="invalid_token", error_description="The token has expired"'
    }


class AuthProviderUnavailableException(SecurityException):
    http_status = 502
