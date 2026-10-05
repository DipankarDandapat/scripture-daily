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

    @property
    def allowed_origins(self) -> list[str]:
        return [origin.strip() for origin in self.cors_origins.split(",") if origin.strip()]


settings = Settings()
