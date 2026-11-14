from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=("../.env", ".env"), env_prefix="SD_", extra="ignore")

    app_name: str = "Scripture Daily API"
    environment: str = "development"
    database_url: str = "sqlite:///./scripture_daily.db"
    jwt_secret: str = "change-this-development-secret-before-deploying"
    jwt_algorithm: str = "HS256"
    access_token_minutes: int = 0  # 0 = no expiry (device tokens are permanent)
    admin_token: str = "local-admin-token-change-me"
    cors_origins: str = "*"
    # Challenges: at most one new day can be completed per calendar day (set false for demos/tests)
    challenge_one_per_day: bool = True
    # Firebase Admin SDK service account JSON file path (optional — FCM push only works when set)
    firebase_credentials_path: str = ""
    # Swagger UI basic-auth credentials
    swagger_username: str = "apiadmin"
    swagger_password: str = "ChangeThisPassword"

    @property
    def allowed_origins(self) -> list[str]:
        return [origin.strip() for origin in self.cors_origins.split(",") if origin.strip()]


settings = Settings()
