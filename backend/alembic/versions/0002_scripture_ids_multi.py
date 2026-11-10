"""Migrate legacy single scripture_id to scripture_ids.

The application bootstrap may already have created the current schema before Alembic runs.
This migration is therefore idempotent for both current and legacy SQLite databases.
"""
import sqlalchemy as sa
from alembic import op

revision = "0002_scripture_ids_multi"
down_revision = "0001_initial"
branch_labels = None
depends_on = None


def upgrade():
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    if not inspector.has_table("user_preferences"):
        return
    columns = {column["name"] for column in inspector.get_columns("user_preferences")}
    if "scripture_ids" in columns:
        return
    if "scripture_id" not in columns:
        # A schema created from current SQLAlchemy metadata already has the multi-ID field.
        return
    op.add_column("user_preferences", sa.Column("scripture_ids", sa.String(255), nullable=True))
    op.execute("UPDATE user_preferences SET scripture_ids = CAST(scripture_id AS VARCHAR)")
    # Legacy installations can have a named FK on PostgreSQL; SQLite batch mode handles
    # the old column without assuming a constraint name.
    if bind.dialect.name == "sqlite":
        with op.batch_alter_table("user_preferences", recreate="always") as batch_op:
            batch_op.drop_column("scripture_id")
            batch_op.alter_column("scripture_ids", nullable=False, server_default="1")
    else:
        with op.batch_alter_table("user_preferences") as batch_op:
            batch_op.alter_column("scripture_ids", nullable=False, server_default="1")
            batch_op.drop_constraint("user_preferences_scripture_id_fkey", type_="foreignkey")
            batch_op.drop_column("scripture_id")


def downgrade():
    # Downgrade to a single scripture is lossy; intentionally left as a no-op.
    pass
