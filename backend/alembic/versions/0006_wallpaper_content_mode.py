"""Persist the wallpaper content mode with user preferences."""
import sqlalchemy as sa
from alembic import op

revision = "0006_wallpaper_content_mode"
down_revision = "0005_active_verse_state"
branch_labels = None
depends_on = None

def upgrade():
    columns = {c["name"] for c in sa.inspect(op.get_bind()).get_columns("user_preferences")}
    if "wallpaper_mode" not in columns:
        op.add_column("user_preferences", sa.Column("wallpaper_mode", sa.String(40), nullable=False, server_default="change_verse"))

def downgrade():
    columns = {c["name"] for c in sa.inspect(op.get_bind()).get_columns("user_preferences")}
    if "wallpaper_mode" in columns:
        op.drop_column("user_preferences", "wallpaper_mode")
