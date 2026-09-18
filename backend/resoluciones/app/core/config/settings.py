"""
Configuracion global del backend del modulo Resoluciones.

Mismo patron que `proyecto-erp/backend/app/core/config/settings.py` (pydantic-settings,
computed fields para ISSUER / JWKS_URL / TOKEN_URL) para que el equipo del ERP lo
reconozca al integrar. Los valores por defecto de Keycloak coinciden con los del ERP
y los de la app movil: realm `alcaldia-idec`, cliente `app-idec`.
"""
from pathlib import Path
from typing import Optional

from pydantic import computed_field
from pydantic_settings import BaseSettings, SettingsConfigDict

# Shared .env with frontend/resoluciones-web (one file instead of two, see
# /.env.example at the repo root) — resolved from this file's own path, not
# from the process cwd, so `python -m app.main` works the same from any cwd.
# This file lives at backend/resoluciones/app/core/config/settings.py, so
# parents[5] is the repo root (backend/resoluciones/app/core/config -> ... -> root).
_SHARED_ENV_FILE = Path(__file__).resolve().parents[5] / ".env"


class Settings(BaseSettings):
    PROJECT_NAME: str = "Resoluciones - Backend"
    VERSION: str = "0.1.0"
    API_V1_STR: str = "/api"

    # Base de datos. Soporta DATABASE_URL directa o variables individuales del ERP (DB_HOST, etc.)
    DATABASE_URL: Optional[str] = None
    DB_HOST: Optional[str] = None
    DB_PORT: Optional[int] = 5432
    DB_USER: Optional[str] = None
    DB_PASSWORD: Optional[str] = None
    DB_NAME: Optional[str] = None

    # Schema de PostgreSQL propio de este dominio (convencion del ERP:
    # "un schema por dominio"). Se ignora en SQLite. "resolutions" porque asi
    # se llama el schema ya creado en la BD real (idec_erp) con las tablas
    # `resolutions` / `resolution_pages` -- ver infrastructure/models.py.
    DB_SCHEMA: str = "resolutions"

    # Keycloak — mismos valores que la app movil y el proyecto-erp.
    KEYCLOAK_URL: str = "https://auth.catastrocbba.com"
    KEYCLOAK_REALM: str = "alcaldia-idec"
    KEYCLOAK_CLIENT_ID: str = "app-idec"
    KEYCLOAK_CLIENT_SECRET: str = ""
    KEYCLOAK_TIMEOUT_SECONDS: int = 20
    # Tolerancia a desincronización de reloj entre servidor local y Keycloak (en segundos)
    JWT_LEEWAY_SECONDS: int = 1800

    # CORS: origen(es) del frontend web. Coma-separado.
    FRONTEND_ORIGIN: str = "http://localhost:5173"

    PORT: int = 8080

    model_config = SettingsConfigDict(
        env_file=str(_SHARED_ENV_FILE),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @computed_field
    @property
    def effective_database_url(self) -> str:
        if self.DATABASE_URL:
            return self.DATABASE_URL
        if self.DB_HOST and self.DB_USER and self.DB_NAME:
            pwd = f":{self.DB_PASSWORD}" if self.DB_PASSWORD else ""
            port = f":{self.DB_PORT}" if self.DB_PORT else ""
            return f"postgresql://{self.DB_USER}{pwd}@{self.DB_HOST}{port}/{self.DB_NAME}"
        return "sqlite:///./resoluciones.db"

    @computed_field
    @property
    def is_sqlite(self) -> bool:
        return self.effective_database_url.startswith("sqlite")

    @computed_field
    @property
    def cors_origins(self) -> list[str]:
        return [o.strip() for o in self.FRONTEND_ORIGIN.split(",") if o.strip()]

    @computed_field
    @property
    def ISSUER(self) -> str:
        return f"{self.KEYCLOAK_URL.rstrip('/')}/realms/{self.KEYCLOAK_REALM}"

    @computed_field
    @property
    def JWKS_URL(self) -> str:
        return f"{self.ISSUER}/protocol/openid-connect/certs"

    @computed_field
    @property
    def TOKEN_URL(self) -> str:
        return f"{self.ISSUER}/protocol/openid-connect/token"


settings = Settings()
