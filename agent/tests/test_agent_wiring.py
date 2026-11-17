"""Wiring tests for the agent node and the MCP tool loading. No backend, no API key.

A scripted chat model stands in for Gemini, so these exercise the real
`create_agent` loop (tool call -> tool result -> structured answer) without a
network call. They check our code reads the loop's result correctly; they say
nothing about how well a real model follows the prompt.
"""

import asyncio

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage
from langchain_core.outputs import ChatGeneration, ChatResult
from langchain_core.tools import tool

from agent.nodes.agent_node import build_agent, build_task_message, make_agent_runner
from agent.schema import DraftReview, HardRuleFlag, Recommendation, RiskLevel
from agent.tools import open_agent_tools


class ScriptedModel(BaseChatModel):
    """Replays a fixed list of responses, one per model call."""

    responses: list[AIMessage]
    calls: int = 0

    @property
    def _llm_type(self) -> str:
        return "scripted"

    def bind_tools(self, tools, **kwargs):  # noqa: ANN001, ANN201
        return self

    def _generate(self, messages, stop=None, run_manager=None, **kwargs):  # noqa: ANN001, ANN201
        message = self.responses[self.calls]
        self.calls += 1
        return ChatResult(generations=[ChatGeneration(message=message)])


@tool
def search_policy(query: str) -> str:
    """Search the leave policy."""
    return "Section 4.2: more than 3 leaves in 30 days requires manager review."


def _state() -> dict:
    return {
        "request_id": 7,
        "locale": "en",
        "rule_floor": RiskLevel.HIGH,
        "rule_flags": [
            HardRuleFlag(code="HIGH_FREQUENCY", level=RiskLevel.HIGH, human_readable="Frequent")
        ],
    }


def test_task_message_carries_the_rule_result():
    message = build_task_message(_state())

    assert "id=7" in message
    assert "HIGH" in message
    assert "HIGH_FREQUENCY" in message


def test_agent_runner_returns_draft_tool_calls_and_tokens():
    usage = {"input_tokens": 100, "output_tokens": 20, "total_tokens": 120}
    model = ScriptedModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[{"name": "search_policy", "args": {"query": "frequent"}, "id": "1"}],
                usage_metadata=usage,
            ),
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "DraftReview",
                        "args": {
                            "summary": "Frequent leave; policy 4.2 applies.",
                            "suggested_risk": "HIGH",
                            "suggested_recommendation": "REVIEW_CAREFULLY",
                            "policy_citations": [{"section": "4.2"}],
                        },
                        "id": "2",
                    }
                ],
                usage_metadata=usage,
            ),
        ]
    )
    run_agent = make_agent_runner(build_agent([search_policy], model=model))

    outcome = asyncio.run(run_agent(_state()))

    assert isinstance(outcome.draft, DraftReview)
    assert outcome.draft.suggested_risk == RiskLevel.HIGH
    assert outcome.draft.suggested_recommendation == Recommendation.REVIEW_CAREFULLY
    assert outcome.draft.policy_citations[0].section == "4.2"
    assert outcome.tool_calls == [("search_policy", True)]
    assert (outcome.input_tokens, outcome.output_tokens) == (200, 40)


def test_agent_is_offered_only_the_two_read_tools():
    # Starts the real MCP server over stdio (it does not contact the backend until
    # a tool is called). evaluate_rules must not be on the agent's menu (I2).
    async def tool_names() -> list[str]:
        async with open_agent_tools() as tools:
            return sorted(t.name for t in tools)

    assert asyncio.run(tool_names()) == ["get_request_context", "search_policy"]
