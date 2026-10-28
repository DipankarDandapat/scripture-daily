from sqlalchemy import create_engine, event
from sqlalchemy.orm import DeclarativeBase, sessionmaker
from app.core.config import settings


class Base(DeclarativeBase):
    pass


_url = settings.database_url
if _url.startswith("sqlite"):
    _connect_args = {"check_same_thread": False}
elif _url.startswith(("postgres", "mysql")):
    _connect_args = {"connect_timeout": 8}
else:
    _connect_args = {}

_connect_args["charset"] = "utf8mb4"

engine = create_engine(
    _url,
    connect_args=_connect_args,
    pool_pre_ping=True,
    pool_timeout=10,
)

if _url.startswith("sqlite"):
    @event.listens_for(engine, "connect")
    def _set_sqlite_pragmas(dbapi_conn, _):
        dbapi_conn.execute("PRAGMA journal_mode=DELETE")
        dbapi_conn.execute("PRAGMA synchronous=NORMAL")

SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
