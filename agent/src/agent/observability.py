"""Lightweight agent-trajectory + usage logging.

Mirrors the backend's provenance philosophy (provider / tokens / latency / failure)
but at the agent layer: which tools were called, in what order, and how long they took.
"""

from __future__ import annotations

import time
from dataclasses import dataclass, field


@dataclass
class ToolCall:
    name: str
    latency_ms: int
    ok: bool
    error: str | None = None


@dataclass
class Trajectory:
    request_id: int
    tool_calls: list[ToolCall] = field(default_factory=list)
    input_tokens: int = 0
    output_tokens: int = 0

    def record(self, name: str, latency_ms: int, ok: bool, error: str | None = None) -> None:
        self.tool_calls.append(ToolCall(name, latency_ms, ok, error))

    @property
    def tool_sequence(self) -> list[str]:
        return [c.name for c in self.tool_calls]


class timed:
    """Context manager returning elapsed milliseconds via `.ms`."""

    def __enter__(self) -> "timed":
        self._start = time.perf_counter()
        return self

    def __exit__(self, *exc: object) -> None:
        self.ms = int((time.perf_counter() - self._start) * 1000)
