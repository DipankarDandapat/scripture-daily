from pydantic import BaseModel, EmailStr, Field, ConfigDict


class AuthInput(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class TokenOut(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserOut(BaseModel):
    id: str
    email: EmailStr
    model_config = ConfigDict(from_attributes=True)


class ScriptureItem(BaseModel):
    id: int
    name: str
    code: str


class ReligionOut(BaseModel):
    id: int
    name: str
    code: str
    scriptures: list[ScriptureItem] = []


class CatalogOut(BaseModel):
    id: int
    name: str
    code: str
    religion: str | None = None
    native_script: str = ""


class VerseOut(BaseModel):
    id: int
    scripture: str
    religion: str
    book: str | None
    chapter: int
    verse_number: int
    language: str
    text: str
    translation: str | None
    source: str
    license: str
    is_demo: bool
    topics: list[str] = []
    explanation_available: bool = False


class PreferenceInput(BaseModel):
    religion_id: int
    language_id: int
    scripture_ids: str = "1"  # comma-separated e.g. "2,3"
    frequency: str = "daily"
    notifications_enabled: bool = False


class PreferenceOut(PreferenceInput):
    model_config = ConfigDict(from_attributes=True)


class FavoriteInput(BaseModel):
    verse_id: int


class DeviceInput(BaseModel):
    device_token: str = Field(min_length=8, max_length=512)
    platform: str = "android"
    device_model: str | None = None


class FeedbackInput(BaseModel):
    rating: int = Field(ge=1, le=10)
    comment: str | None = Field(default=None, max_length=1000)
    consent: bool = False


class FeedbackOut(BaseModel):
    id: int
    rating: int
    comment: str | None
    consent: bool
    model_config = ConfigDict(from_attributes=True)


class VerseCreate(BaseModel):
    scripture_id: int
    language_id: int
    book: str | None = None
    chapter: int = Field(ge=1)
    verse_number: int = Field(ge=1)
    text: str = Field(min_length=1)
    translation: str | None = None
    source: str = ""
    license: str = ""
    is_active: bool = True


# ── Feature schemas ──────────────────────────────────────────────────────────
class MoodOut(BaseModel):
    code: str
    label: str
    emoji: str
    message: str


class MoodVerseOut(BaseModel):
    mood: str
    message: str
    verse: VerseOut


class MoodCheckinInput(BaseModel):
    mood: str
    verse_id: int | None = None


class MoodCheckinOut(BaseModel):
    id: int
    mood: str
    verse_id: int | None
    created_at: str


class MoodSummaryOut(BaseModel):
    days: int
    total: int
    counts: dict[str, int]
    top_mood: str | None
    recent: list[MoodCheckinOut]


class ExplanationOut(BaseModel):
    verse_id: int
    level: str
    language: str
    text: str


class TopicOut(BaseModel):
    code: str
    name: str
    icon: str
    description: str
    religion_count: int
    verse_count: int


class TruthCard(BaseModel):
    religion_id: int
    religion: str
    religion_code: str
    language_fallback: bool
    verse: VerseOut


class UniversalTruthOut(BaseModel):
    topic: TopicOut
    language: str
    cards: list[TruthCard]


class ChallengeProgressOut(BaseModel):
    joined: bool = False
    completed_days: list[int] = []
    next_day: int | None = 1
    completed: bool = False
    badge_earned: bool = False
    available_tomorrow: bool = False  # today's day is done and the next one unlocks on the next calendar day
    completed_count: int = 0
    remaining_days: int = 0
    percent: int = 0
    last_completed_on: str | None = None
    missed_days: int = 0  # whole calendar days since the last completed day (0 = on track). Never punitive: nothing resets.


class ChallengeOut(BaseModel):
    id: int
    code: str
    title: str
    subtitle: str
    description: str
    total_days: int
    icon: str
    color: str
    badge_name: str
    badge_icon: str
    progress: ChallengeProgressOut
    is_universal: bool = False
    language: str = "en"
    language_fallback: bool = False


class ChallengeDayOut(BaseModel):
    day_number: int
    title: str
    reflection: str
    action: str
    topic: str
    status: str  # completed | available | locked
    verse: VerseOut | None = None


class ChallengeDetailOut(ChallengeOut):
    days: list[ChallengeDayOut]


class CompleteDayInput(BaseModel):
    note: str | None = Field(default=None, max_length=500)


class CompleteDayOut(BaseModel):
    challenge_id: int
    day_number: int
    progress: ChallengeProgressOut
    badge_earned_now: bool
    badge_name: str | None = None
    badge_icon: str | None = None


class BadgeOut(BaseModel):
    challenge_id: int
    challenge_code: str
    challenge_title: str
    badge_name: str
    badge_icon: str
    color: str
    earned_at: str


class StorySceneOut(BaseModel):
    scene_number: int
    art: str
    text: str


class StoryOut(BaseModel):
    id: int
    code: str
    emoji: str
    palette: str
    values: list[str]
    title: str
    lesson: str
    summary: str = ""
    religion_code: str = ""
    language: str
    language_fallback: bool
    scripture: str
    reference: str
    verse_id: int | None
    scene_count: int


class StoryDetailOut(StoryOut):
    scenes: list[StorySceneOut]
    verse: VerseOut | None = None


# ── Studio templates + Wisdom Tree ───────────────────────────────────────────
class TemplateOut(BaseModel):
    code: str
    name: str
    kind: str
    tradition: str | None  # None = common
    motif: str
    color_top: str
    color_bottom: str
    color_text: str
    color_accent: str
    color_muted: str
    is_light: bool
    emoji: str
    sort_order: int
    model_config = ConfigDict(from_attributes=True)


class TreeMilestone(BaseModel):
    stage: str
    label: str
    level: int
    first_day: int
    last_day: int
    unlocked: bool


class TreeNext(BaseModel):
    stage: str
    label: str
    level: int
    at_days: int
    days_to_go: int


class TreeOut(BaseModel):
    total_days: int
    level: int
    stage: str
    stage_label: str
    status: str
    current_streak: int
    longest_streak: int
    read_today: bool
    last_read: str | None
    next_milestone: TreeNext | None
    milestones: list[TreeMilestone]
    history: list[str]


class TreeReadInput(BaseModel):
    dates: list[str] = Field(default_factory=list, max_length=400)  # local dates (YYYY-MM-DD) completed on this device
    mark_today: bool = True  # False = only merge `dates` (a pure sync that does not count today)
