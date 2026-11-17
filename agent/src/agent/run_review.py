"""Full agent review entrypoint.

Usage:  python -m agent.run_review --request-id 1

Needs a running backend, the AI_AGENT password and a Gemini API key (see
.env.example). For a backend-only connectivity check, run `python -m agent.smoke`.
"""

from __future__ import annotations

import argparse
import asyncio

import httpx

from .backend_client import BackendClient
from .graph import ReviewState, build_graph
from .nodes.agent_node import AgentOutcome, build_agent, make_agent_runner
from .tools import open_agent_tools


class RuleEngineUnavailable(RuntimeError):
    """The backend could not give a rule floor. No floor, no review."""


async def run_review(request_id: int, locale: str = "en") -> ReviewState:
    """Run one governed review end to end and return the final graph state."""
    backend_error: httpx.HTTPError | None = None
    with BackendClient() as client:
        async with open_agent_tools() as tools:

            async def run_agent(state: dict) -> AgentOutcome:
                # Built inside the agent node on purpose: a model that cannot even be
                # constructed (no API key, bad model name) is an agent failure like any
                # other, so the review falls back to the rules instead of crashing.
                return await make_agent_runner(build_agent(tools))(state)

            graph = build_graph(client, run_agent)
            try:
                return await graph.ainvoke({"request_id": request_id, "locale": locale})
            except httpx.HTTPError as exc:
                # Held until the MCP session has closed: raised from in here it would
                # surface wrapped in the session's ExceptionGroup.
                backend_error = exc
    raise RuleEngineUnavailable(
        f"Cannot get a rule evaluation from the backend: {backend_error}"
    ) from backend_error


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the governed agent review")
    parser.add_argument("--request-id", type=int, required=True)
    parser.add_argument("--locale", default="en", choices=["en", "zh-TW"])
    args = parser.parse_args()

    try:
        state = asyncio.run(run_review(args.request_id, args.locale))
    except RuleEngineUnavailable as exc:
        raise SystemExit(str(exc)) from exc

    print(state["final"].model_dump_json(indent=2))
    trajectory = state["trajectory"]
    print(f"\ntools called: {' -> '.join(trajectory.tool_sequence)}")
    print(f"tokens: {trajectory.input_tokens} in / {trajectory.output_tokens} out")
    if state.get("agent_error"):
        print(f"agent fell back to rules: {state['agent_error']}")


if __name__ == "__main__":
    main()
