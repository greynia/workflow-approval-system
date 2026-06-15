"""Governed LLM agent (LangGraph + MCP) for the leave-approval workflow.

The agent autonomously uses governed backend tools to draft a leave review,
but safety-critical risk decisions stay deterministic: a rule-evaluation node
always runs before the agent, and a rule-floor node always runs after it, so the
agent can never lower risk below the rule engine's floor.

See docs/02_ai_agent_langgraph_mcp_plan.md for the full design.
"""

__version__ = "0.1.0"
