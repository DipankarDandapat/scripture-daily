"""Persist one active verse per installation identity.

Revision ID: 0005_active_verse_state
Revises: 0004_wisdomone_v2_enhancements
"""
import sqlalchemy as sa
from alembic import op

revision = "0005_active_verse_state"
down_revision = "0004_wisdomone_v2_enhancements"
branch_labels = None
depends_on = None


def upgrade():
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    if not inspector.has_table("active_verse_states"):
        op.create_table(
            "active_verse_states",
            sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
            sa.Column("verse_id", sa.Integer, sa.ForeignKey("verses.id", ondelete="CASCADE"), nullable=False),
            sa.Column("religion_id", sa.Integer, sa.ForeignKey("religions.id"), nullable=False),
            sa.Column("language_id", sa.Integer, sa.ForeignKey("languages.id"), nullable=False),
            sa.Column("selected_at", sa.DateTime(timezone=True), nullable=False),
            sa.Column("next_update_at", sa.DateTime(timezone=True), nullable=False),
            sa.Column("selection_source", sa.String(20), nullable=False, server_default="scheduled"),
        )
    inspector = sa.inspect(bind)
    active_indexes = {index["name"] for index in inspector.get_indexes("active_verse_states")}
    for name, column in (
        ("ix_active_verse_states_verse_id", "verse_id"),
        ("ix_active_verse_states_religion_id", "religion_id"),
        ("ix_active_verse_states_language_id", "language_id"),
        ("ix_active_verse_states_next_update_at", "next_update_at"),
    ):
        if name not in active_indexes:
            op.create_index(name, "active_verse_states", [column])

    if not inspector.has_table("verse_selection_history"):
        op.create_table(
            "verse_selection_history",
            sa.Column("id", sa.Integer, primary_key=True),
            sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
            sa.Column("verse_id", sa.Integer, sa.ForeignKey("verses.id", ondelete="CASCADE"), nullable=False),
            sa.Column("selected_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.Column("selection_source", sa.String(20), nullable=False, server_default="scheduled"),
        )
    history_indexes = {index["name"] for index in sa.inspect(bind).get_indexes("verse_selection_history")}
    for name, column in (
        ("ix_verse_selection_history_user_id", "user_id"),
        ("ix_verse_selection_history_verse_id", "verse_id"),
    ):
        if name not in history_indexes:
            op.create_index(name, "verse_selection_history", [column])


def downgrade():
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    if inspector.has_table("verse_selection_history"):
        op.drop_table("verse_selection_history")
    if sa.inspect(bind).has_table("active_verse_states"):
        op.drop_table("active_verse_states")
