"""
Obtiene y cachea en memoria las claves publicas (JWKS) de Keycloak.
Copia fiel de `proyecto-erp/backend/app/core/security/jwks_service.py`.
"""
from typing import Any, Dict, Optional

import requests

from app.core.config import settings
from app.core.security.exceptions import (
    AuthProviderUnavailableException,
    TokenVerificationException,
)


class JWKSService:
    def __init__(self, jwks_url: Optional[str] = None, timeout: Optional[int] = None):
        self._jwks_url = jwks_url or settings.JWKS_URL
        self._timeout = timeout or settings.KEYCLOAK_TIMEOUT_SECONDS
        self._cache: Optional[Dict[str, Any]] = None

    def fetch_jwks(self, force_refresh: bool = False) -> Dict[str, Any]:
        if self._cache is None or force_refresh:
            try:
                response = requests.get(self._jwks_url, timeout=self._timeout)
                response.raise_for_status()
                self._cache = response.json()
            except requests.RequestException as exc:
                raise AuthProviderUnavailableException(
                    f"No se pudo obtener las claves publicas de Keycloak desde {self._jwks_url}: {exc}"
                ) from exc
        return self._cache

    def get_signing_key(self, kid: Optional[str]) -> Dict[str, Any]:
        if not kid:
            raise TokenVerificationException("El encabezado del token no contiene 'kid'")

        jwks = self.fetch_jwks()
        key = self._find_key_in_jwks(jwks, kid)

        if key is None:
            jwks = self.fetch_jwks(force_refresh=True)
            key = self._find_key_in_jwks(jwks, kid)

        if key is None:
            raise TokenVerificationException(
                f"No se encontro la clave de firma para kid='{kid}' en Keycloak"
            )
        return key

    @staticmethod
    def _find_key_in_jwks(jwks: Dict[str, Any], kid: str) -> Optional[Dict[str, Any]]:
        return next((k for k in jwks.get("keys", []) if k.get("kid") == kid), None)
