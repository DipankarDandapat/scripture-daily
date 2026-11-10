<<<<<<< HEAD
"""WisdomOne features: moods, topics, challenges, stories and simple explanations.

Revision ID: 0003_wisdomone_features
=======
"""Ensure WisdomOne feature tables and columns exist.

The app's SQLAlchemy bootstrap creates the current metadata for fresh installations.
For an existing older database this revision adds the one legacy column and creates
all missing feature tables without dropping existing data.
>>>>>>> settings_redesign
"""
import sqlalchemy as sa
from alembic import op

revision = "0003_wisdomone_features"
down_revision = "0002_scripture_ids_multi"
branch_labels = None
depends_on = None


def upgrade():
<<<<<<< HEAD
    op.add_column("verses", sa.Column("simple_explanation", sa.Text(), nullable=True))
    op.create_table("app_meta", sa.Column("key", sa.String(60), primary_key=True), sa.Column("value", sa.String(255), nullable=False, server_default=""))
    op.create_table(
        "topics", sa.Column("id", sa.Integer, primary_key=True), sa.Column("code", sa.String(40), nullable=False, unique=True),
        sa.Column("name", sa.String(80), nullable=False), sa.Column("icon", sa.String(16), nullable=False, server_default=""),
        sa.Column("description", sa.String(240), nullable=False, server_default=""), sa.Column("sort_order", sa.Integer, nullable=False, server_default="0"))
    op.create_table(
        "verse_topics", sa.Column("verse_id", sa.Integer, sa.ForeignKey("verses.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("topic_id", sa.Integer, sa.ForeignKey("topics.id", ondelete="CASCADE"), primary_key=True))
    op.create_index("ix_verse_topics_topic_id", "verse_topics", ["topic_id"])
    op.create_table(
        "verse_moods", sa.Column("verse_id", sa.Integer, sa.ForeignKey("verses.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("mood", sa.String(20), primary_key=True))
    op.create_index("ix_verse_moods_mood", "verse_moods", ["mood"])
    op.create_table(
        "mood_checkins", sa.Column("id", sa.Integer, primary_key=True),
        sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("mood", sa.String(20), nullable=False), sa.Column("verse_id", sa.Integer, sa.ForeignKey("verses.id", ondelete="SET NULL")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()))
    op.create_table(
        "challenges", sa.Column("id", sa.Integer, primary_key=True), sa.Column("code", sa.String(60), nullable=False, unique=True),
        sa.Column("title", sa.String(120), nullable=False), sa.Column("subtitle", sa.String(200), nullable=False, server_default=""),
        sa.Column("description", sa.Text, nullable=False), sa.Column("total_days", sa.Integer, nullable=False),
        sa.Column("icon", sa.String(16), nullable=False, server_default=""), sa.Column("color", sa.String(9), nullable=False, server_default="#D4A574"),
        sa.Column("badge_name", sa.String(80), nullable=False, server_default=""), sa.Column("badge_icon", sa.String(16), nullable=False, server_default=""),
        sa.Column("sort_order", sa.Integer, nullable=False, server_default="0"), sa.Column("is_active", sa.Boolean, nullable=False, server_default=sa.true()))
    op.create_table(
        "challenge_days", sa.Column("id", sa.Integer, primary_key=True),
        sa.Column("challenge_id", sa.Integer, sa.ForeignKey("challenges.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("day_number", sa.Integer, nullable=False), sa.Column("title", sa.String(120), nullable=False),
        sa.Column("reflection", sa.Text, nullable=False), sa.Column("action", sa.Text, nullable=False),
        sa.Column("topic_id", sa.Integer, sa.ForeignKey("topics.id"), nullable=False), sa.UniqueConstraint("challenge_id", "day_number", name="uq_challenge_day"))
    op.create_table(
        "user_challenges", sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("challenge_id", sa.Integer, sa.ForeignKey("challenges.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("started_at", sa.DateTime(timezone=True), server_default=sa.func.now()), sa.Column("completed_at", sa.DateTime(timezone=True)))
    op.create_table(
        "challenge_progress", sa.Column("id", sa.Integer, primary_key=True),
        sa.Column("user_id", sa.String(10), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("challenge_id", sa.Integer, sa.ForeignKey("challenges.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("day_number", sa.Integer, nullable=False), sa.Column("completed_on", sa.Date, nullable=False), sa.Column("note", sa.Text),
        sa.UniqueConstraint("user_id", "challenge_id", "day_number", name="uq_challenge_progress"))
    op.create_table(
        "stories", sa.Column("id", sa.Integer, primary_key=True), sa.Column("code", sa.String(60), nullable=False, unique=True),
        sa.Column("emoji", sa.String(16), nullable=False, server_default=""), sa.Column("palette", sa.String(20), nullable=False, server_default="dusk"),
        sa.Column("moral_values", sa.String(120), nullable=False, server_default=""),
        sa.Column("scripture_id", sa.Integer, sa.ForeignKey("scriptures.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("book", sa.String(120)), sa.Column("chapter", sa.Integer, nullable=False), sa.Column("verse_number", sa.Integer, nullable=False),
        sa.Column("sort_order", sa.Integer, nullable=False, server_default="0"))
    op.create_index("ix_story_ref", "stories", ["scripture_id", "book", "chapter", "verse_number"])
    op.create_table(
        "story_translations", sa.Column("id", sa.Integer, primary_key=True),
        sa.Column("story_id", sa.Integer, sa.ForeignKey("stories.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("language_id", sa.Integer, sa.ForeignKey("languages.id"), nullable=False, index=True),
        sa.Column("title", sa.String(200), nullable=False), sa.Column("summary", sa.Text, nullable=False), sa.Column("lesson", sa.Text, nullable=False),
        sa.UniqueConstraint("story_id", "language_id", name="uq_story_lang"))
    op.create_table(
        "story_scenes", sa.Column("id", sa.Integer, primary_key=True),
        sa.Column("story_id", sa.Integer, sa.ForeignKey("stories.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("scene_number", sa.Integer, nullable=False), sa.Column("art", sa.String(40), nullable=False),
        sa.UniqueConstraint("story_id", "scene_number", name="uq_story_scene"))
    op.create_table(
        "story_scene_translations", sa.Column("id", sa.Integer, primary_key=True),
        sa.Column("scene_id", sa.Integer, sa.ForeignKey("story_scenes.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("language_id", sa.Integer, sa.ForeignKey("languages.id"), nullable=False, index=True),
        sa.Column("text", sa.Text, nullable=False), sa.UniqueConstraint("scene_id", "language_id", name="uq_scene_lang"))


def downgrade():
    for table in ("story_scene_translations", "story_scenes", "story_translations", "stories", "challenge_progress", "user_challenges",
                  "challenge_days", "challenges", "mood_checkins", "verse_moods", "verse_topics", "topics", "app_meta"):
        op.drop_table(table)
    op.drop_column("verses", "simple_explanation")
=======
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
>>>>>>> settings_redesign
