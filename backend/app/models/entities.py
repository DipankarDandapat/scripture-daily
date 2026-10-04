from datetime import datetime
from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class Religion(Base):
    __tablename__ = "religions"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(80), unique=True)
    code: Mapped[str] = mapped_column(String(40), unique=True)
    scriptures: Mapped[list["Scripture"]] = relationship(back_populates="religion")


class Language(Base):
    __tablename__ = "languages"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(80), unique=True)
    code: Mapped[str] = mapped_column(String(12), unique=True)


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
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    scripture: Mapped[Scripture] = relationship()
    language: Mapped[Language] = relationship()


class User(Base):
    __tablename__ = "users"
    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(String(320), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    preferences: Mapped["UserPreference | None"] = relationship(back_populates="user", uselist=False, cascade="all, delete-orphan")


class UserPreference(Base):
    __tablename__ = "user_preferences"
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    religion_id: Mapped[int] = mapped_column(ForeignKey("religions.id"), default=1)
    language_id: Mapped[int] = mapped_column(ForeignKey("languages.id"), default=1)
    scripture_id: Mapped[int] = mapped_column(ForeignKey("scriptures.id"), default=1)
    frequency: Mapped[str] = mapped_column(String(20), default="daily")
    notifications_enabled: Mapped[bool] = mapped_column(Boolean, default=False)
    user: Mapped[User] = relationship(back_populates="preferences")


class Favorite(Base):
    __tablename__ = "favorites"
    __table_args__ = (UniqueConstraint("user_id", "verse_id", name="uq_user_favorite"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    verse_id: Mapped[int] = mapped_column(ForeignKey("verses.id", ondelete="CASCADE"), index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    verse: Mapped[Verse] = relationship()


class Device(Base):
    __tablename__ = "devices"
    __table_args__ = (UniqueConstraint("user_id", "device_token", name="uq_user_device_token"),)
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    device_token: Mapped[str] = mapped_column(String(512))
    platform: Mapped[str] = mapped_column(String(30), default="android")
    device_model: Mapped[str | None] = mapped_column(String(120), nullable=True)
