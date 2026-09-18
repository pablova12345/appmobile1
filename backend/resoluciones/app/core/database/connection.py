"""
Conexion a la base de datos. Mismo patron que
`proyecto-erp/backend/app/core/database/connection.py` (SQLite o PostgreSQL segun
DATABASE_URL, `Base.metadata.create_all()` al arrancar -- todavia sin Alembic,
igual que el ERP).

Diferencia: cuando la URL es PostgreSQL, los modelos ORM viven en el schema
`settings.DB_SCHEMA` ("resoluciones") -- convencion "un schema por dominio" del
ERP. En SQLite ese schema se ignora (SQLite no tiene schemas).
"""
from typing import Generator

from sqlalchemy import create_engine, event
from sqlalchemy.orm import Session, declarative_base, sessionmaker
from sqlalchemy.schema import CreateSchema

from app.core.config import settings

db_uri = settings.effective_database_url

if settings.is_sqlite:
    engine = create_engine(db_uri, connect_args={"check_same_thread": False})
else:
    engine = create_engine(db_uri, pool_pre_ping=True, pool_recycle=3600)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# En Postgres, los modelos declaran schema=settings.DB_SCHEMA (ver
# infrastructure/models.py). En SQLite no se pasa schema.
Base = declarative_base()

if not settings.is_sqlite:

    @event.listens_for(engine, "connect", insert=True)
    def _set_search_path(dbapi_connection, connection_record):  # pragma: no cover
        cursor = dbapi_connection.cursor()
        cursor.execute(f'SET search_path TO "{settings.DB_SCHEMA}", public')
        cursor.close()


def init_db_tables() -> bool:
    """Crea el schema (Postgres) y las tablas si no existen."""
    try:
        from app.domains.resoluciones.infrastructure import models  # noqa: F401

        if not settings.is_sqlite:
            with engine.begin() as conn:
                conn.execute(CreateSchema(settings.DB_SCHEMA, if_not_exists=True))

        Base.metadata.create_all(bind=engine)
        return True
    except Exception as exc:  # pragma: no cover
        import logging

        logging.getLogger("uvicorn.error").warning(f"Aviso al inicializar la BD: {exc}")
        return False


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
