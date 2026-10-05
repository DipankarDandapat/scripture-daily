"""Migrate user_preferences.scripture_id (FK int) to scripture_ids (varchar).

Revision ID: 0002_scripture_ids_multi
"""
import sqlalchemy as sa
from alembic import op

revision = "0002_scripture_ids_multi"
down_revision = "0001_initial"
branch_labels = None
depends_on = None


def upgrade():
    with op.batch_alter_table("user_preferences") as batch_op:
        batch_op.add_column(sa.Column("scripture_ids", sa.String(255), nullable=True))
    # Copy existing single ID to new column as string
    op.execute("UPDATE user_preferences SET scripture_ids = CAST(scripture_id AS VARCHAR)")
    with op.batch_alter_table("user_preferences") as batch_op:
        batch_op.alter_column("scripture_ids", nullable=False, server_default="1")
        batch_op.drop_constraint("user_preferences_scripture_id_fkey", type_="foreignkey")
        batch_op.drop_column("scripture_id")


def downgrade():
    with op.batch_alter_table("user_preferences") as batch_op:
        batch_op.add_column(sa.Column("scripture_id", sa.Integer(), nullable=True))
    op.execute("UPDATE user_preferences SET scripture_id = CAST(SPLIT_PART(scripture_ids, ',', 1) AS INTEGER)")
    with op.batch_alter_table("user_preferences") as batch_op:
        batch_op.alter_column("scripture_id", nullable=False)
        batch_op.drop_column("scripture_ids")
