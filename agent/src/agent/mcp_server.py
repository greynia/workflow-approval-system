"""MCP server exposing the governed backend tools to any MCP-capable agent.

Run (stdio):  python -m agent.mcp_server
Inspect:      npx @modelcontextprotocol/inspector python -m agent.mcp_server

These three tools are the agent's entire action surface. The tool docstrings are
the "skill definitions" the LLM reads to decide when to call each one.
"""

from __future__ import annotations

from mcp.server.fastmcp import FastMCP

from .backend_client import BackendClient

mcp = FastMCP("workflow-approval-tools")
_client = BackendClient()


@mcp.tool()
def get_request_context(request_id: int) -> dict:
    """Fetch a leave request's context: applicant tenure/role/department, leave
    type, duration, reason, recent-leave stats, and the approval steps.
    Call this first to understand what is being reviewed."""
    return _client.get_request_context(request_id)


@mcp.tool()
def evaluate_rules(request_id: int, locale: str = "en") -> dict:
    """Run the deterministic rule engine for a request. Returns the authoritative
    hard-rule flags and the risk floor (`riskLevel`). This is the source of truth
    for risk — your final recommendation must not go below it."""
    return _client.evaluate_rules(request_id, locale=locale)


@mcp.tool()
def search_policy(query: str, top_k: int = 5, locale: str | None = None) -> dict:
    """Semantic search over company leave policy (pgvector + optional rerank).
    Use it to ground your reasoning and cite policy sources when risk flags are
    present. Returns matching policy excerpts with sources."""
    return _client.search_policy(query, top_k=top_k, locale=locale)


if __name__ == "__main__":
    mcp.run()
