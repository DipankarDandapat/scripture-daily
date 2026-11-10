from contextlib import asynccontextmanager  # noqa: F401
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.core.database import Base, SessionLocal, engine
<<<<<<< HEAD
=======
from app.core.firebase import init_firebase
>>>>>>> settings_redesign
from app.models import *  # noqa: F403 — ensure all ORM models register with Base.metadata
from app.seed import seed_database
from app.api.v1.routes import router
from app.api.v1.features import router as features_router
from app.api.v1.studio import router as studio_router
<<<<<<< HEAD
=======
from app.api.v1.journal import router as journal_router
from app.api.v1.config import router as config_router
from app.services.app_config import ensure_app_config
from app.services.reflections import ensure_reflection_questions
>>>>>>> settings_redesign


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


def _add_missing_columns() -> None:
    """Non-destructive column additions for databases created before a column existed (any dialect)."""
    from sqlalchemy import inspect, text
    insp = inspect(engine)
    if "verses" in insp.get_table_names():
        cols = {c["name"] for c in insp.get_columns("verses")}
        if "simple_explanation" not in cols:
            with engine.begin() as conn:
                conn.execute(text("ALTER TABLE verses ADD COLUMN simple_explanation TEXT"))
    if "challenges" in insp.get_table_names():
        cols = {c["name"] for c in insp.get_columns("challenges")}
        if "is_universal" not in cols:
            with engine.begin() as conn:
                conn.execute(text("ALTER TABLE challenges ADD COLUMN is_universal BOOLEAN NOT NULL DEFAULT 0"))
<<<<<<< HEAD
=======
    if "user_preferences" in insp.get_table_names():
        cols = {c["name"] for c in insp.get_columns("user_preferences")}
        if "wallpaper_mode" not in cols:
            with engine.begin() as conn:
                conn.execute(text("ALTER TABLE user_preferences ADD COLUMN wallpaper_mode VARCHAR(40) NOT NULL DEFAULT 'verse_and_image'"))
        # Legacy wallpaper modes (change_verse / keep_same_verse) are replaced by verse_and_image / keep_image / colors.
        with engine.begin() as conn:
            conn.execute(text("UPDATE user_preferences SET wallpaper_mode = 'verse_and_image' WHERE wallpaper_mode IN ('change_verse', 'keep_same_verse')"))
    if "devices" in insp.get_table_names():
        cols = {c["name"] for c in insp.get_columns("devices")}
        if "fcm_token" not in cols:
            with engine.begin() as conn:
                conn.execute(text("ALTER TABLE devices ADD COLUMN fcm_token VARCHAR(512)"))
>>>>>>> settings_redesign


# Run at import time so Passenger WSGI and uvicorn both initialise the schema
_ensure_schema()
Base.metadata.create_all(bind=engine)
_add_missing_columns()
with SessionLocal() as _db:
    seed_database(_db)
<<<<<<< HEAD
=======
    ensure_reflection_questions(_db)
    ensure_app_config(_db)

init_firebase(settings.firebase_credentials_path)
>>>>>>> settings_redesign


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(title=settings.app_name, version="2.0.0", description="Content-driven daily scripture API", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=settings.allowed_origins, allow_credentials=True, allow_methods=["*"], allow_headers=["*"])
app.include_router(router, prefix="/api/v1")
app.include_router(features_router, prefix="/api/v1")
app.include_router(studio_router, prefix="/api/v1")
<<<<<<< HEAD
=======
app.include_router(journal_router, prefix="/api/v1")
app.include_router(config_router, prefix="/api/v1")
>>>>>>> settings_redesign


@app.get("/")
def root():
    return {"name": settings.app_name, "docs": "/docs", "api": "/api/v1"}
