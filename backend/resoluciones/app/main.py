"""
App FastAPI del modulo Resoluciones. Mismo esqueleto que
`proyecto-erp/backend/app/main.py` (lifespan -> init_db_tables, CORS desde
settings, exception handlers, `api_router` bajo prefix /api).
"""
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.core.database.connection import init_db_tables
from app.core.errors.handlers import register_exception_handlers
from app.registry import api_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db_tables()
    yield


def create_application() -> FastAPI:
    application = FastAPI(
        title=settings.PROJECT_NAME,
        version=settings.VERSION,
        description="Modulo Resoluciones (RELACION DE SUPERFICIE -> Hoja2). Backend de guardado; OCR y Excel se hacen en el frontend web.",
        lifespan=lifespan,
    )

    application.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    register_exception_handlers(application)
    application.include_router(api_router, prefix=settings.API_V1_STR)

    @application.get("/health", tags=["health"])
    def health():
        return {"ok": True, "service": settings.PROJECT_NAME, "db": settings.effective_database_url.split("://")[0]}

    return application


app = create_application()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=settings.PORT, reload=True)
