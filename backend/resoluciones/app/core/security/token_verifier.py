"""
Verifica el JWT de Keycloak por firma JWKS y extrae la identidad.
Equivalente a `KeycloakAdapter.verify_token` + `_map_payload_to_user_profile`
del proyecto-erp, reducido a lo que este modulo necesita (sub + username).
"""
import logging
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional

from jose import jwt

from app.core.config import settings
from app.core.security.exceptions import TokenExpiredException, TokenVerificationException
from app.core.security.jwks_service import JWKSService

logger = logging.getLogger("uvicorn.error")


@dataclass
class VerifiedUser:
    sub: str
    username: str
    email: Optional[str] = None
    roles: List[str] = field(default_factory=list)
    claims: Dict[str, Any] = field(default_factory=dict)


class TokenVerifier:
    def __init__(self, jwks_service: Optional[JWKSService] = None):
        self._jwks = jwks_service or JWKSService()
        self._issuer = settings.ISSUER
        self._leeway = settings.JWT_LEEWAY_SECONDS

    def verify(self, token: str) -> VerifiedUser:
        if not token:
            raise TokenVerificationException("Token vacio o no proporcionado")

        try:
            header = jwt.get_unverified_header(token)
        except Exception as exc:
            logger.warning("Token con encabezado invalido: %s", exc)
            raise TokenVerificationException("Token con formato o encabezado invalido") from exc

        signing_key = self._jwks.get_signing_key(header.get("kid"))

        try:
            payload = jwt.decode(
                token,
                signing_key,
                algorithms=["RS256", "RS384", "RS512", "ES256", "PS256"],
                issuer=self._issuer,
                # Keycloak pone aud="account" para el cliente por defecto; no lo
                # exigimos (mismo criterio que el proyecto-erp: verify_aud=False).
                options={
                    "verify_aud": False,
                    "leeway": self._leeway,
                },
            )
        except jwt.ExpiredSignatureError as exc:
            logger.warning("Token expirado (verifique sincronizacion de hora local vs Keycloak): %s", exc)
            raise TokenExpiredException("El token ha expirado") from exc
        except jwt.JWTClaimsError as exc:
            logger.warning("Claims del token invalidos: %s", exc)
            raise TokenVerificationException(f"Claims del token invalidos: {exc}") from exc
        except Exception as exc:
            logger.warning("Error al decodificar y validar token: %s", exc)
            raise TokenVerificationException(f"Token invalido: {exc}") from exc

        sub = payload.get("sub")
        if not sub:
            # Sin `sub` no hay a quien atribuir la resolucion (no soportamos el
            # "usuario institucional" del ERP aca).
            raise TokenVerificationException("El token no trae 'sub' (identidad de usuario)")

        return VerifiedUser(
            sub=sub,
            username=payload.get("preferred_username") or sub,
            email=payload.get("email"),
            roles=payload.get("realm_access", {}).get("roles", []),
            claims=payload,
        )
