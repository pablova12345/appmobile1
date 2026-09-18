"""
Dependencia `get_current_user` para proteger endpoints. Valida el Bearer JWT de
Keycloak por firma JWKS y devuelve `VerifiedUser` (sub + username).

Al integrar al ERP: reemplazar los imports `from app.auth.deps import get_current_user`
por `from app.domains.seguridad.presentation.deps import get_current_user` (su
version devuelve un UserProfile con RBAC; para este modulo alcanza con `sub`).
"""
from functools import lru_cache

from fastapi import Depends, Security
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.security.jwks_service import JWKSService
from app.core.security.token_verifier import TokenVerifier, VerifiedUser

_bearer = HTTPBearer()


@lru_cache
def _get_verifier() -> TokenVerifier:
    return TokenVerifier(jwks_service=JWKSService())


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Security(_bearer),
    verifier: TokenVerifier = Depends(_get_verifier),
) -> VerifiedUser:
    return verifier.verify(credentials.credentials)
