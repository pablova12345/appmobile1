"""
Cliente ROPC contra Keycloak (grant_type=password / refresh_token). Equivale a
`KeycloakAdapter.authenticate_credentials` / `refresh_token` del proyecto-erp,
recortado. El client secret vive solo aca (backend), nunca llega al navegador.
"""
from typing import Any, Dict

import requests

from app.core.config import settings
from app.core.security.exceptions import (
    AuthProviderUnavailableException,
    InvalidCredentialsException,
)


def _post_token(data: Dict[str, str]) -> Dict[str, Any]:
    data = {**data, "client_id": settings.KEYCLOAK_CLIENT_ID}
    if settings.KEYCLOAK_CLIENT_SECRET:
        data["client_secret"] = settings.KEYCLOAK_CLIENT_SECRET

    try:
        resp = requests.post(
            settings.TOKEN_URL, data=data, timeout=settings.KEYCLOAK_TIMEOUT_SECONDS
        )
    except requests.RequestException as exc:
        raise AuthProviderUnavailableException("No se pudo contactar a Keycloak") from exc

    if resp.status_code == 200:
        return resp.json()

    try:
        err = resp.json()
        desc = err.get("error_description") or err.get("error") or ""
    except Exception:
        desc = resp.text

    if "invalid user credentials" in desc.lower():
        raise InvalidCredentialsException("Usuario o contraseña incorrectos.")
    raise InvalidCredentialsException(desc or f"Error de autenticacion (HTTP {resp.status_code}).")


def login(username: str, password: str) -> Dict[str, Any]:
    return _post_token({"grant_type": "password", "username": username, "password": password})


def refresh(refresh_token: str) -> Dict[str, Any]:
    return _post_token({"grant_type": "refresh_token", "refresh_token": refresh_token})
