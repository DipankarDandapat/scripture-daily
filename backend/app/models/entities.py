import secrets
import string
from datetime import date, datetime
from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Index, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base

_UID_CHARS = string.ascii_letters + string.digits


def _new_uid() -> str:
    return "".join(secrets.choice(_UID_CHARS) for _ in range(10))


class Religion(Base):
    __tablename__ = "religions"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(80), unique=True)
    code: Mapped[str] = mapped_column(String(40), unique=True)
    scriptures: Mapped[list["Scripture"]] = relationship(back_populates="religion")
    languages: Mapped[list["Language"]] = relationship(secondary="religion_languages", back_populates="religions")


class Language(Base):
    __tablename__ = "languages"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(80), unique=True)
    code: Mapped[str] = mapped_column(String(12), unique=True)
    native_script: Mapped[str] = mapped_column(String(80), default="")
    religions: Mapped[list["Religion"]] = relationship(secondary="religion_languages", back_populates="languages")


class ReligionLanguage(Base):
    __tablename__ = "religion_languages"
    __table_args__ = (UniqueConstraint("religion_id", "language_id", name="uq_religion_language"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    religion_id: Mapped[int] = mapped_column(ForeignKey("religions.id", ondelete="CASCADE"), index=True)
    language_id: Mapped[int] = mapped_column(ForeignKey("languages.id", ondelete="CASCADE"), index=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)


class Scripture(Base):
    __tablename__ = "scriptures"
    id: Mapped[int] = mapped_column(primary_key=True)
    religion_id: Mapped[int] = mapped_column(ForeignKey("religions.id", ondelete="CASCADE"), index=True)
    name: Mapped[str] = mapped_column(String(120))
    code: Mapped[str] = mapped_column(String(60), unique=True)
    religion: Mapped[Religion] = relationship(back_populates="scriptures")


class Verse(Base):
    __tablename__ = "verses"
    __table_args__ = (UniqueConstraint("scripture_id", "language_id", "book", "chapter", "verse_number", name="uq_verse_reference"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    scripture_id: Mapped[int] = mapped_column(ForeignKey("scriptures.id", ondelete="CASCADE"), index=True)
    language_id: Mapped[int] = mapped_column(ForeignKey("languages.id"), index=True)
    book: Mapped[str | None] = mapped_column(String(120), nullable=True)
    chapter: Mapped[int] = mapped_column(Integer)
    verse_number: Mapped[int] = mapped_column(Integer)
    text: Mapped[str] = mapped_column(Text)
    translation: Mapped[str | None] = mapped_column(Text, nullable=True)
    source: Mapped[str] = mapped_column(String(240), default="")
    license: Mapped[str] = mapped_column(String(160), default="")
    is_demo: Mapped[bool] = mapped_column(Boolean, default=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, index=True)
    # "Explain this to me like I'm 5" — simple modern explanation in the verse's own language
    simple_explanation: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    scripture: Mapped[Scripture] = relationship()
    language: Mapped[Language] = relationship()
    topics: Mapped[list["Topic"]] = relationship(secondary="verse_topics", lazy="selectin", viewonly=True)
    moods: Mapped[list["VerseMood"]] = relationship(lazy="selectin", viewonly=True)


class User(Base):
    __tablename__ = "users"
    id: Mapped[str] = mapped_column(String(10), primary_key=True, default=_new_uid)
    email: Mapped[str] = mapped_column(String(320), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    preferences: Mapped["UserPreference | None"] = relationship(back_populates="user", uselist=False, cascade="all, delete-orphan")


class UserPreference(Base):
    __tablename__ = "user_preferences"
    user_id: Mapped[str] = mapped_column(String(10), ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    religion_id: Mapped[int] = mapped_column(ForeignKey("religions.id"), default=1)
    language_id: Mapped[int] = mapped_column(ForeignKey("languages.id"), default=1)
    scripture_ids: Mapped[str] = mapped_column(String(255), default="1")
    frequency: Mapped[str] = mapped_column(String(20), default="daily")
    notifications_enabled: Mapped[bool] = mapped_column(Boolean, default=False)
    user: Mapped[User] = relationship(back_populates="preferences")


class Favorite(Base):
    __tablename__ = "favorites"
    __table_args__ = (UniqueConstraint("user_id", "verse_id", name="uq_user_favorite"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[str] = mapped_column(String(10), ForeignKey("users.id", ondelete="CASCADE"), index=True)
    verse_id: Mapped[int] = mapped_column(ForeignKey("verses.id", ondelete="CASCADE"), index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    verse: Mapped[Verse] = relationship()


class Device(Base):
    __tablename__ = "devices"
    __table_args__ = (UniqueConstraint("user_id", "device_token", name="uq_user_device_token"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[str] = mapped_column(String(10), ForeignKey("users.id", ondelete="CASCADE"), index=True)
    device_token: Mapped[str] = mapped_column(String(512))
    platform: Mapped[str] = mapped_column(String(30), default="android")
    device_model: Mapped[str | None] = mapped_column(String(120), nullable=True)


class Feedback(Base):
    __tablename__ = "feedback"
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[str] = mapped_column(String(10), ForeignKey("users.id", ondelete="CASCADE"), index=True)
    rating: Mapped[int] = mapped_column(Integer)
    comment: Mapped[str | None] = mapped_column(Text, nullable=True)
    consent: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    user: Mapped[User] = relationship()


# ── Feature tables: moods, universal truths, challenges, stories ─────────────

MOODS = ("anxious", "happy", "sad", "angry")


class AppMeta(Base):
    """Key/value store (e.g. the content version the verse catalog was last seeded with)."""
    __tablename__ = "app_meta"
    key: Mapped[str] = mapped_column(String(60), primary_key=True)
    value: Mapped[str] = mapped_column(String(255), default="")


class Topic(Base):
    """A cross-tradition theme (love, patience, …) used by Universal Truths and challenges."""
    __tablename__ = "topics"
    id: Mapped[int] = mapped_column(primary_key=True)
    code: Mapped[str] = mapped_column(String(40), unique=True)
    name: Mapped[str] = mapped_column(String(80))
    icon: Mapped[str] = mapped_column(String(16), default="")
    description: Mapped[str] = mapped_column(String(240), default="")
    sort_order: Mapped[int] = mapped_column(Integer, default=0)


class VerseTopic(Base):
    __tablename__ = "verse_topics"
    verse_id: Mapped[int] = mapped_column(ForeignKey("verses.id", ondelete="CASCADE"), primary_key=True)
    topic_id: Mapped[int] = mapped_column(ForeignKey("topics.id", ondelete="CASCADE"), primary_key=True, index=True)


class VerseMood(Base):
    __tablename__ = "verse_moods"
    verse_id: Mapped[int] = mapped_column(ForeignKey("verses.id", ondelete="CASCADE"), primary_key=True)
    mood: Mapped[str] = mapped_column(String(20), primary_key=True, index=True)


class MoodCheckin(Base):
    __tablename__ = "mood_checkins"
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[str] = mapped_column(String(10), ForeignKey("users.id", ondelete="CASCADE"), index=True)
    mood: Mapped[str] = mapped_column(String(20))
    verse_id: Mapped[int | None] = mapped_column(ForeignKey("verses.id", ondelete="SET NULL"), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class Challenge(Base):
    __tablename__ = "challenges"
    id: Mapped[int] = mapped_column(primary_key=True)
    code: Mapped[str] = mapped_column(String(60), unique=True)
    title: Mapped[str] = mapped_column(String(120))
    subtitle: Mapped[str] = mapped_column(String(200), default="")
    description: Mapped[str] = mapped_column(Text, default="")
    total_days: Mapped[int] = mapped_column(Integer)
    icon: Mapped[str] = mapped_column(String(16), default="")
    color: Mapped[str] = mapped_column(String(9), default="#D4A574")
    badge_name: Mapped[str] = mapped_column(String(80), default="")
    badge_icon: Mapped[str] = mapped_column(String(16), default="")
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    # Universal challenges may mix verses from every tradition; all others stay inside the user's tradition.
    is_universal: Mapped[bool] = mapped_column(Boolean, default=False)
    translations: Mapped[list["ChallengeTranslation"]] = relationship(cascade="all, delete-orphan")
    days: Mapped[list["ChallengeDay"]] = relationship(back_populates="challenge", order_by="ChallengeDay.day_number", cascade="all, delete-orphan")


class ChallengeDay(Base):
    __tablename__ = "challenge_days"
    __table_args__ = (UniqueConstraint("challenge_id", "day_number", name="uq_challenge_day"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    challenge_id: Mapped[int] = mapped_column(ForeignKey("challenges.id", ondelete="CASCADE"), index=True)
    day_number: Mapped[int] = mapped_column(Integer)
    title: Mapped[str] = mapped_column(String(120))
    reflection: Mapped[str] = mapped_column(Text)
    action: Mapped[str] = mapped_column(Text)
    topic_id: Mapped[int] = mapped_column(ForeignKey("topics.id"))
    challenge: Mapped[Challenge] = relationship(back_populates="days")
    topic: Mapped[Topic] = relationship()


class ChallengeTranslation(Base):
    """Localised title/subtitle/description/badge for a challenge (English lives on the challenge row)."""
    __tablename__ = "challenge_translations"
    __table_args__ = (UniqueConstraint("challenge_id", "language_id", name="uq_challenge_lang"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    challenge_id: Mapped[int] = mapped_column(ForeignKey("challenges.id", ondelete="CASCADE"), index=True)
    language_id: Mapped[int] = mapped_column(ForeignKey("languages.id"), index=True)
    title: Mapped[str] = mapped_column(String(160))
    subtitle: Mapped[str] = mapped_column(String(240), default="")
    description: Mapped[str] = mapped_column(Text, default="")
    badge_name: Mapped[str] = mapped_column(String(120), default="")


class ChallengeDayTranslation(Base):
    __tablename__ = "challenge_day_translations"
    __table_args__ = (UniqueConstraint("day_id", "language_id", name="uq_challenge_day_lang"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    day_id: Mapped[int] = mapped_column(ForeignKey("challenge_days.id", ondelete="CASCADE"), index=True)
    language_id: Mapped[int] = mapped_column(ForeignKey("languages.id"), index=True)
    title: Mapped[str] = mapped_column(String(160))
    reflection: Mapped[str] = mapped_column(Text)
    action: Mapped[str] = mapped_column(Text)


class UserChallenge(Base):
    """A user's enrolment in a challenge. completed_at set => badge earned."""
    __tablename__ = "user_challenges"
    user_id: Mapped[str] = mapped_column(String(10), ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    challenge_id: Mapped[int] = mapped_column(ForeignKey("challenges.id", ondelete="CASCADE"), primary_key=True)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    challenge: Mapped[Challenge] = relationship()


class ChallengeProgress(Base):
    __tablename__ = "challenge_progress"
    __table_args__ = (UniqueConstraint("user_id", "challenge_id", "day_number", name="uq_challenge_progress"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[str] = mapped_column(String(10), ForeignKey("users.id", ondelete="CASCADE"), index=True)
    challenge_id: Mapped[int] = mapped_column(ForeignKey("challenges.id", ondelete="CASCADE"), index=True)
    day_number: Mapped[int] = mapped_column(Integer)
    completed_on: Mapped[date] = mapped_column(Date)
    note: Mapped[str | None] = mapped_column(Text, nullable=True)


class Story(Base):
    """A child-friendly story attached to a specific verse reference."""
    __tablename__ = "stories"
    __table_args__ = (Index("ix_story_ref", "scripture_id", "book", "chapter", "verse_number"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    code: Mapped[str] = mapped_column(String(60), unique=True)
    emoji: Mapped[str] = mapped_column(String(16), default="")
    palette: Mapped[str] = mapped_column(String(20), default="dusk")
    moral_values: Mapped[str] = mapped_column(String(120), default="")
    scripture_id: Mapped[int] = mapped_column(ForeignKey("scriptures.id", ondelete="CASCADE"), index=True)
    book: Mapped[str | None] = mapped_column(String(120), nullable=True)
    chapter: Mapped[int] = mapped_column(Integer)
    verse_number: Mapped[int] = mapped_column(Integer)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    scripture: Mapped[Scripture] = relationship()
    translations: Mapped[list["StoryTranslation"]] = relationship(cascade="all, delete-orphan")
    scenes: Mapped[list["StoryScene"]] = relationship(order_by="StoryScene.scene_number", cascade="all, delete-orphan")


class StoryTranslation(Base):
    __tablename__ = "story_translations"
    __table_args__ = (UniqueConstraint("story_id", "language_id", name="uq_story_lang"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    story_id: Mapped[int] = mapped_column(ForeignKey("stories.id", ondelete="CASCADE"), index=True)
    language_id: Mapped[int] = mapped_column(ForeignKey("languages.id"), index=True)
    title: Mapped[str] = mapped_column(String(200))
    summary: Mapped[str] = mapped_column(Text, default="")
    lesson: Mapped[str] = mapped_column(Text, default="")


class StoryScene(Base):
    __tablename__ = "story_scenes"
    __table_args__ = (UniqueConstraint("story_id", "scene_number", name="uq_story_scene"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    story_id: Mapped[int] = mapped_column(ForeignKey("stories.id", ondelete="CASCADE"), index=True)
    scene_number: Mapped[int] = mapped_column(Integer)
    art: Mapped[str] = mapped_column(String(40))  # key of the vector illustration the app draws
    translations: Mapped[list["StorySceneTranslation"]] = relationship(cascade="all, delete-orphan")


class StorySceneTranslation(Base):
    __tablename__ = "story_scene_translations"
    __table_args__ = (UniqueConstraint("scene_id", "language_id", name="uq_scene_lang"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    scene_id: Mapped[int] = mapped_column(ForeignKey("story_scenes.id", ondelete="CASCADE"), index=True)
    language_id: Mapped[int] = mapped_column(ForeignKey("languages.id"), index=True)
    text: Mapped[str] = mapped_column(Text)


# ── Studio templates + Wisdom Tree ───────────────────────────────────────────

class StudioTemplate(Base):
    """A Share/Video Studio template. ``tradition`` NULL = common (shown to everyone); otherwise a religion code.

    Rows are data: add or retire a template without shipping a new app build. ``motif`` names a vector
    illustration the app draws (om, lotus, cross, geometry, mountains, …); colours are #RRGGBB.
    """
    __tablename__ = "studio_templates"
    id: Mapped[int] = mapped_column(primary_key=True)
    code: Mapped[str] = mapped_column(String(60), unique=True)
    name: Mapped[str] = mapped_column(String(80))
    kind: Mapped[str] = mapped_column(String(10), default="both")  # share | video | both
    tradition: Mapped[str | None] = mapped_column(String(40), nullable=True, index=True)
    motif: Mapped[str] = mapped_column(String(30), default="none")
    color_top: Mapped[str] = mapped_column(String(9))
    color_bottom: Mapped[str] = mapped_column(String(9))
    color_text: Mapped[str] = mapped_column(String(9))
    color_accent: Mapped[str] = mapped_column(String(9))
    color_muted: Mapped[str] = mapped_column(String(9))
    is_light: Mapped[bool] = mapped_column(Boolean, default=False)
    emoji: Mapped[str] = mapped_column(String(16), default="")
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, index=True)


class ReadingDay(Base):
    """One row per user per local calendar day on which the daily reading was completed (Wisdom Tree)."""
    __tablename__ = "reading_days"
    __table_args__ = (UniqueConstraint("user_id", "day", name="uq_reading_day"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[str] = mapped_column(String(10), ForeignKey("users.id", ondelete="CASCADE"), index=True)
    day: Mapped[date] = mapped_column(Date)
