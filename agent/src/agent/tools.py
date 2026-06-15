"""Load the MCP tools as LangChain tools for the agent.  [Phase 2 — STUB]

Intended implementation:

    from langchain_mcp_adapters.client import MultiServerMCPClient

    client = MultiServerMCPClient({
        "workflow": {
            "command": "python",
            "args": ["-m", "agent.mcp_server"],
            "transport": "stdio",
        }
    })
    tools = await client.get_tools()   # -> get_request_context / evaluate_rules / search_policy
"""

from __future__ import annotations


async def load_mcp_tools() -> list:
    raise NotImplementedError(
        "Phase 2: load MCP tools via langchain-mcp-adapters. "
        "See docs/02_ai_agent_langgraph_mcp_plan.md §6."
    )
