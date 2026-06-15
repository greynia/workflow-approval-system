"""Typed settings loaded from environment / .env (see .env.example)."""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    # Backend
    backend_base_url: str = "http://localhost:8080"
    agent_email: str = "ai-agent@system.local"
    agent_password: str = ""
    agent_preferred_locale: str = "en"  # en | zh-TW
    request_timeout_seconds: float = 30.0

    # LLM (used from Phase 2 onward)
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.0-flash"


@lru_cache
def get_settings() -> Settings:
    return Settings()
