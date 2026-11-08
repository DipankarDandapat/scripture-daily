"""journal reflections, verse reflection questions, wisdom tree settings/activities, app config, wallpaper modes

Revision ID: 0008_journal_tree_config
Revises: 0007_fcm_token
Create Date: 2026-11-08 00:00:00

Idempotent: the app's startup ``create_all`` may already have created these tables.
"""
from alembic import op
import sqlalchemy as sa

revision = "0008_journal_tree_config"
down_revision = "0007_fcm_token"
branch_labels = None
depends_on = None


def _missing(name: str) -> bool:
    return not sa.inspect(op.get_bind()).has_table(name)


def upgrade() -> None:
    if _missing("verse_reflection_questions"):
        op.create_table(
            "verse_reflection_questions",
            sa.Column("id", sa.Integer, primary_key=True),
            sa.Column("verse_id", sa.Integer, sa.ForeignKey("verses.id", ondelete="CASCADE"), nullable=False),
            sa.Column("scripture_id", sa.Integer, sa.ForeignKey("scriptures.id", ondelete="CASCADE"), nullable=False),
            sa.Column("language_id", sa.Integer, sa.ForeignKey("languages.id"), nullable=False),
            sa.Column("reflection_question", sa.Text, nullable=False),
            sa.Column("is_active", sa.Boolean, nullable=False, server_default=sa.true()),
            sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
            sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
            sa.UniqueConstraint("verse_id", name="uq_reflection_question_verse"),
        )
        op.create_index("ix_verse_reflection_questions_verse_id", "verse_reflection_questions", ["verse_id"])
        op.create_index("ix_verse_reflection_questions_scripture_id", "verse_reflection_questions", ["scripture_id"])
        op.create_index("ix_verse_reflection_questions_language_id", "verse_reflection_questions", ["language_id"])
        op.create_index("ix_verse_reflection_questions_is_active", "verse_reflection_questions", ["is_active"])
    if _missing("journal_reflections"):
        op.create_table(
            "journal_reflections",
            sa.Column("id", sa.Integer, primary_key=True),
            sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
            sa.Column("verse_id", sa.Integer, sa.ForeignKey("verses.id", ondelete="SET NULL"), nullable=True),
            sa.Column("reflection_question_id", sa.Integer, sa.ForeignKey("verse_reflection_questions.id", ondelete="SET NULL"), nullable=True),
            sa.Column("reflection_date", sa.Date, nullable=False),
            sa.Column("reflection_answer", sa.Text, nullable=False),
            sa.Column("question_text", sa.Text, nullable=False),
            sa.Column("verse_text", sa.Text, nullable=False, server_default=""),
            sa.Column("verse_reference", sa.String(240), nullable=False, server_default=""),
            sa.Column("scripture_name", sa.String(120), nullable=False, server_default=""),
            sa.Column("language_code", sa.String(12), nullable=False, server_default="en"),
            sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
            sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
            sa.UniqueConstraint("user_id", "verse_id", "reflection_date", name="uq_reflection_user_verse_day"),
        )
        op.create_index("ix_journal_reflections_user_id", "journal_reflections", ["user_id"])
        op.create_index("ix_reflection_user_date", "journal_reflections", ["user_id", "reflection_date"])
    if _missing("tree_settings"):
        op.create_table(
            "tree_settings",
            sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
            sa.Column("enabled", sa.Boolean, nullable=False, server_default=sa.true()),
            sa.Column("started_on", sa.Date, nullable=True),
            sa.Column("reset_count", sa.Integer, nullable=False, server_default="0"),
            sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
        )
    if _missing("tree_activity_done"):
        op.create_table(
            "tree_activity_done",
            sa.Column("id", sa.Integer, primary_key=True),
            sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
            sa.Column("code", sa.String(40), nullable=False),
            sa.Column("completed_on", sa.Date, nullable=False),
            sa.UniqueConstraint("user_id", "code", name="uq_tree_activity_done"),
        )
        op.create_index("ix_tree_activity_done_user_id", "tree_activity_done", ["user_id"])
    if _missing("tree_activity_events"):
        op.create_table(
            "tree_activity_events",
            sa.Column("id", sa.Integer, primary_key=True),
            sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
            sa.Column("event", sa.String(30), nullable=False),
            sa.Column("day", sa.Date, nullable=False),
            sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
        )
        op.create_index("ix_tree_activity_events_user_id", "tree_activity_events", ["user_id"])
        op.create_index("ix_tree_event_user_event_day", "tree_activity_events", ["user_id", "event", "day"])
    if _missing("app_config"):
        op.create_table(
            "app_config",
            sa.Column("key", sa.String(120), primary_key=True),
            sa.Column("value", sa.Text, nullable=False),
            sa.Column("description", sa.String(300), nullable=False, server_default=""),
            sa.Column("is_public", sa.Boolean, nullable=False, server_default=sa.true()),
            sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
        )
    op.execute("UPDATE user_preferences SET wallpaper_mode = 'verse_and_image' WHERE wallpaper_mode IN ('change_verse', 'keep_same_verse')")


def downgrade() -> None:
    for table in ("app_config", "tree_activity_events", "tree_activity_done", "tree_settings", "journal_reflections", "verse_reflection_questions"):
        if not _missing(table):
            op.drop_table(table)
