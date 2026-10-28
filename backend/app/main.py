from contextlib import asynccontextmanager  # noqa: F401
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.core.database import Base, SessionLocal, engine
from app.models import *  # noqa: F403 — ensure all ORM models register with Base.metadata
from app.seed import seed_database
from app.api.v1.routes import router


def _ensure_schema() -> None:
    """For SQLite dev mode: migrate schema changes without dropping data."""
    if not settings.database_url.startswith("sqlite"):
        return
    from sqlalchemy import inspect, text
    insp = inspect(engine)
    if "users" not in insp.get_table_names():
        return  # fresh DB — create_all will handle it
    # Drop and rebuild if old integer user PK or missing religion_languages table
    cols = {c["name"]: c for c in insp.get_columns("users")}
    user_id_col = cols.get("id", {})
    old_int_pk = str(user_id_col.get("type", "")).upper().startswith("INTEGER")
    missing_rl_table = "religion_languages" not in insp.get_table_names()
    if old_int_pk or missing_rl_table:
        Base.metadata.drop_all(bind=engine)
        return
    # Add missing columns to existing tables (non-destructive)
    if "languages" in insp.get_table_names():
        lang_cols = {c["name"] for c in insp.get_columns("languages")}
        with engine.connect() as conn:
            if "native_script" not in lang_cols:
                conn.execute(text("ALTER TABLE languages ADD COLUMN native_script VARCHAR(80) NOT NULL DEFAULT ''"))
                conn.commit()


# Run at import time so Passenger WSGI and uvicorn both initialise the schema
_ensure_schema()
Base.metadata.create_all(bind=engine)
with SessionLocal() as _db:
    seed_database(_db)


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(title=settings.app_name, version="1.0.0", description="Content-driven daily scripture API", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=settings.allowed_origins, allow_credentials=True, allow_methods=["*"], allow_headers=["*"])
app.include_router(router, prefix="/api/v1")


@app.get("/")
def root():
    return {"name": settings.app_name, "docs": "/docs", "api": "/api/v1"}
