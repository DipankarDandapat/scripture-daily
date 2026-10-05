from pydantic import BaseModel, EmailStr, Field, ConfigDict


class AuthInput(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class TokenOut(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserOut(BaseModel):
    id: int
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
