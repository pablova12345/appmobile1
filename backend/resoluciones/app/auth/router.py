"""
Router de autenticacion standalone: /api/login, /api/refresh, /api/me.
Mismos paths que el frontend del proyecto-erp (`API_ENDPOINTS.AUTH`).
"""
from fastapi import APIRouter, Depends

from app.auth import keycloak_client
from app.auth.deps import get_current_user
from app.auth.schemas import LoginRequest, MeResponse, RefreshRequest, TokenResponse
from app.core.security.token_verifier import VerifiedUser

router = APIRouter(tags=["auth"])


@router.post("/login", response_model=TokenResponse)
def login(body: LoginRequest) -> TokenResponse:
    data = keycloak_client.login(body.username.strip(), body.password)
    return TokenResponse(
        access_token=data["access_token"],
        refresh_token=data.get("refresh_token"),
        expires_in=data.get("expires_in"),
    )


@router.post("/refresh", response_model=TokenResponse)
def refresh(body: RefreshRequest) -> TokenResponse:
    data = keycloak_client.refresh(body.refresh_token)
    return TokenResponse(
        access_token=data["access_token"],
        refresh_token=data.get("refresh_token"),
        expires_in=data.get("expires_in"),
    )


@router.get("/me", response_model=MeResponse)
def me(user: VerifiedUser = Depends(get_current_user)) -> MeResponse:
    return MeResponse(sub=user.sub, username=user.username, email=user.email, roles=user.roles)
