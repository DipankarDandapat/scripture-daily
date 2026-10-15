from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.core.database import Base, SessionLocal, engine
from app.models import *  # noqa: F403 — ensure all ORM models register with Base.metadata
from app.seed import seed_database
from app.api.v1.routes import router


def _ensure_schema() -> None:
    """For SQLite dev mode: if the users.id column is still INTEGER (old schema),
    drop all tables and let create_all rebuild them with the current models.
    This is safe because SQLite is local-only; never runs against PostgreSQL."""
    if not settings.database_url.startswith("sqlite"):
        return
    from sqlalchemy import inspect, text
    insp = inspect(engine)
    if "users" not in insp.get_table_names():
        return  # fresh DB — create_all will handle it
    cols = {c["name"]: c for c in insp.get_columns("users")}
    user_id_col = cols.get("id", {})
    # SQLAlchemy reflects SQLite INTEGER PK as type 'INTEGER'
    if str(user_id_col.get("type", "")).upper().startswith("INTEGER"):
        Base.metadata.drop_all(bind=engine)


@asynccontextmanager
async def lifespan(app: FastAPI):
    _ensure_schema()
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        seed_database(db)
    yield


app = FastAPI(title=settings.app_name, version="1.0.0", description="Content-driven daily scripture API", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=settings.allowed_origins, allow_credentials=True, allow_methods=["*"], allow_headers=["*"])
app.include_router(router, prefix="/api/v1")


@app.get("/")
def root():
    return {"name": settings.app_name, "docs": "/docs", "api": "/api/v1"}
