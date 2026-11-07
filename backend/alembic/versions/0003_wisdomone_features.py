"""Ensure WisdomOne feature tables and columns exist.

The app's SQLAlchemy bootstrap creates the current metadata for fresh installations.
For an existing older database this revision adds the one legacy column and creates
all missing feature tables without dropping existing data.
"""
import sqlalchemy as sa
from alembic import op

revision = "0003_wisdomone_features"
down_revision = "0002_scripture_ids_multi"
branch_labels = None
depends_on = None


def upgrade():
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    if inspector.has_table("verses"):
        columns = {column["name"] for column in inspector.get_columns("verses")}
        if "simple_explanation" not in columns:
            op.add_column("verses", sa.Column("simple_explanation", sa.Text(), nullable=True))
    from app.core.database import Base
    from app import models  # noqa: F401
    Base.metadata.create_all(bind=bind)


def downgrade():
    # Feature tables may have been created by the application's schema bootstrap.
    # Avoid destructive downgrades that could remove user reading/challenge data.
    pass
