"""Agent node — the LLM-driven tool loop (Gemini over MCP tools).

Produces a DraftReview. It is sandwiched between the two deterministic nodes, so
its output is only ever a *suggestion*; enforce_floor has the final say.

Two things live here:

  build_agent(tools)        the LangChain agent: model + tools + system prompt,
                            with DraftReview as the required output shape
  make_agent_runner(agent)  wraps that agent as the `run_agent(state)` callable
                            the graph expects, and reads back what it did
                            (tool calls, token usage)

The graph only depends on the `run_agent(state)` shape, so tests swap in a fake.
"""

from __future__ import annotations

import json
from collections.abc import Awaitable, Callable
from dataclasses import dataclass, field

from ..config import Settings, get_settings
from ..schema import DraftReview

SYSTEM_PROMPT = (
    "You are a leave-approval review assistant. Use the provided tools to draft a "
    "review. The deterministic rule engine owns the risk floor; you only advise. "
    "When risk flags are present you MUST call search_policy and cite a source. "
    "Call get_request_context when you need the details of the request. "
    "Only cite policy text that a tool actually returned."
)

# Upper bound on model/tool steps inside one agent run. A review needs a handful
# of calls; hitting this means the model is looping, and the graph falls back.
AGENT_RECURSION_LIMIT = 12


@dataclass
class AgentOutcome:
    """What one agent run produced. `draft` is None when no valid draft came back."""

    draft: DraftReview | None
    tool_calls: list[tuple[str, bool]] = field(default_factory=list)  # (name, ok)
    input_tokens: int = 0
    output_tokens: int = 0


AgentRunner = Callable[[dict], Awaitable[AgentOutcome]]


def build_model(settings: Settings | None = None):  # noqa: ANN201 - returns a chat model
    from langchain_google_genai import ChatGoogleGenerativeAI

    settings = settings or get_settings()
    return ChatGoogleGenerativeAI(
        model=settings.gemini_model,
        google_api_key=settings.gemini_api_key,
        temperature=0,
    )


def build_agent(tools: list, model=None):  # noqa: ANN001, ANN201 - returns a compiled agent
    """Model + tools + prompt. The answer must be a DraftReview."""
    from langchain.agents import create_agent

    return create_agent(
        model if model is not None else build_model(),
        tools,
        system_prompt=SYSTEM_PROMPT,
        response_format=DraftReview,
    )


def build_task_message(state: dict) -> str:
    """The per-request instruction: which request, and what the rules already said."""
    flags = [
        {"code": f.code, "level": f.level.value, "reason": f.human_readable}
        for f in state.get("rule_flags", [])
    ]
    return (
        f"Review leave request id={state['request_id']}.\n"
        f"Write the review in locale: {state.get('locale', 'en')}.\n"
        f"Rule engine risk floor: {state['rule_floor'].value}\n"
        f"Rule flags: {json.dumps(flags, ensure_ascii=False)}"
    )


def make_agent_runner(agent) -> AgentRunner:  # noqa: ANN001 - a compiled LangChain agent
    async def run_agent(state: dict) -> AgentOutcome:
        result = await agent.ainvoke(
            {"messages": [{"role": "user", "content": build_task_message(state)}]},
            config={"recursion_limit": AGENT_RECURSION_LIMIT},
        )
        outcome = AgentOutcome(draft=result.get("structured_response"))
        for message in result.get("messages", []):
            # The final answer can arrive as a call to a pseudo-tool named after the
            # output schema. That is the answer's format, not an action the agent took.
            if message.type == "tool" and message.name != DraftReview.__name__:
                ok = getattr(message, "status", "success") != "error"
                outcome.tool_calls.append((message.name or "unknown", ok))
            usage = getattr(message, "usage_metadata", None)
            if usage:
                outcome.input_tokens += usage.get("input_tokens", 0)
                outcome.output_tokens += usage.get("output_tokens", 0)
        return outcome

    return run_agent
