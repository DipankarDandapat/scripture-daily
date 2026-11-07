"""Ensure WisdomOne v2 tables and columns exist without destructive recreation."""
import sqlalchemy as sa
from alembic import op

revision = "0004_wisdomone_v2_enhancements"
down_revision = "0003_wisdomone_features"
branch_labels = None
depends_on = None


def upgrade():
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    if inspector.has_table("challenges"):
        columns = {column["name"] for column in inspector.get_columns("challenges")}
        if "is_universal" not in columns:
            op.add_column("challenges", sa.Column("is_universal", sa.Boolean(), nullable=False, server_default=sa.false()))
    from app.core.database import Base
    from app import models  # noqa: F401
    Base.metadata.create_all(bind=bind)


def downgrade():
    # The revision is an idempotent schema reconciliation step; preserve user data.
    pass
