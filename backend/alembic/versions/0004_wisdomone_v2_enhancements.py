"""WisdomOne v2 enhancements: studio templates, Wisdom Tree reading days, localised + universal challenges.

Revision ID: 0004_wisdomone_v2_enhancements
"""
import sqlalchemy as sa
from alembic import op

revision = "0004_wisdomone_v2_enhancements"
down_revision = "0003_wisdomone_features"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("challenges", sa.Column("is_universal", sa.Boolean(), nullable=False, server_default=sa.false()))
    op.create_table(
        "challenge_translations", sa.Column("id", sa.Integer, primary_key=True),
        sa.Column("challenge_id", sa.Integer, sa.ForeignKey("challenges.id", ondelete="CASCADE"), nullable=False),
        sa.Column("language_id", sa.Integer, sa.ForeignKey("languages.id"), nullable=False),
        sa.Column("title", sa.String(160), nullable=False), sa.Column("subtitle", sa.String(240), nullable=False, server_default=""),
        sa.Column("description", sa.Text, nullable=False, server_default=""), sa.Column("badge_name", sa.String(120), nullable=False, server_default=""),
        sa.UniqueConstraint("challenge_id", "language_id", name="uq_challenge_lang"))
    op.create_index("ix_challenge_translations_challenge_id", "challenge_translations", ["challenge_id"])
    op.create_index("ix_challenge_translations_language_id", "challenge_translations", ["language_id"])
    op.create_table(
        "challenge_day_translations", sa.Column("id", sa.Integer, primary_key=True),
        sa.Column("day_id", sa.Integer, sa.ForeignKey("challenge_days.id", ondelete="CASCADE"), nullable=False),
        sa.Column("language_id", sa.Integer, sa.ForeignKey("languages.id"), nullable=False),
        sa.Column("title", sa.String(160), nullable=False), sa.Column("reflection", sa.Text, nullable=False), sa.Column("action", sa.Text, nullable=False),
        sa.UniqueConstraint("day_id", "language_id", name="uq_challenge_day_lang"))
    op.create_index("ix_challenge_day_translations_day_id", "challenge_day_translations", ["day_id"])
    op.create_index("ix_challenge_day_translations_language_id", "challenge_day_translations", ["language_id"])
    op.create_table(
        "studio_templates", sa.Column("id", sa.Integer, primary_key=True), sa.Column("code", sa.String(60), nullable=False, unique=True),
        sa.Column("name", sa.String(80), nullable=False), sa.Column("kind", sa.String(10), nullable=False, server_default="both"),
        sa.Column("tradition", sa.String(40), nullable=True), sa.Column("motif", sa.String(30), nullable=False, server_default="none"),
        sa.Column("color_top", sa.String(9), nullable=False), sa.Column("color_bottom", sa.String(9), nullable=False),
        sa.Column("color_text", sa.String(9), nullable=False), sa.Column("color_accent", sa.String(9), nullable=False),
        sa.Column("color_muted", sa.String(9), nullable=False), sa.Column("is_light", sa.Boolean, nullable=False, server_default=sa.false()),
        sa.Column("emoji", sa.String(16), nullable=False, server_default=""), sa.Column("sort_order", sa.Integer, nullable=False, server_default="0"),
        sa.Column("is_active", sa.Boolean, nullable=False, server_default=sa.true()))
    op.create_index("ix_studio_templates_tradition", "studio_templates", ["tradition"])
    op.create_index("ix_studio_templates_is_active", "studio_templates", ["is_active"])
    op.create_table(
        "reading_days", sa.Column("id", sa.Integer, primary_key=True),
        sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("day", sa.Date, nullable=False), sa.UniqueConstraint("user_id", "day", name="uq_reading_day"))
    op.create_index("ix_reading_days_user_id", "reading_days", ["user_id"])


def downgrade():
    for table in ("reading_days", "studio_templates", "challenge_day_translations", "challenge_translations"):
        op.drop_table(table)
    op.drop_column("challenges", "is_universal")
