"""Typed settings loaded from environment / .env (see .env.example)."""

from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# Resolve .env relative to the agent project root (this file is at
# agent/src/agent/config.py) so it is found regardless of the current working
# directory — smoke test, MCP server and graph may each launch from a different cwd.
_AGENT_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=_AGENT_ROOT / ".env", env_file_encoding="utf-8", extra="ignore"
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
