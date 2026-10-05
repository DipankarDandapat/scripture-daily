from datetime import datetime, timedelta, timezone
import jwt
from pwdlib import PasswordHash
from app.core.config import settings

_passwords = PasswordHash.recommended()


def hash_password(password: str) -> str:
    return _passwords.hash(password)


def verify_password(password: str, hashed: str) -> bool:
    return _passwords.verify(password, hashed)


def create_access_token(user_id: int) -> str:
    # Device tokens are long-lived (10 years). No exp claim = never expires.
    payload: dict = {"sub": str(user_id)}
    if settings.access_token_minutes > 0:
        expires = datetime.now(timezone.utc) + timedelta(minutes=settings.access_token_minutes)
        payload["exp"] = expires
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm)
