"""add fcm_token to devices

Revision ID: 0007_fcm_token
Revises: 0006_wallpaper_content_mode
Create Date: 2025-01-01 00:00:00
"""
from alembic import op
import sqlalchemy as sa

revision = "0007_fcm_token"
down_revision = "0006_wallpaper_content_mode"
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Idempotent: a fresh database created from the current models already has the column.
    insp = sa.inspect(op.get_bind())
    if "fcm_token" in {c["name"] for c in insp.get_columns("devices")}:
        return
    with op.batch_alter_table("devices") as batch_op:
        batch_op.add_column(sa.Column("fcm_token", sa.String(512), nullable=True))
        batch_op.create_index("ix_devices_fcm_token", ["fcm_token"])


def downgrade() -> None:
    with op.batch_alter_table("devices") as batch_op:
        batch_op.drop_index("ix_devices_fcm_token")
        batch_op.drop_column("fcm_token")
