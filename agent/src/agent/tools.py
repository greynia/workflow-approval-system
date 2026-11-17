"""Load the governed MCP tools as LangChain tools for the agent node.

The agent talks to the same MCP server Claude Code uses (Head A), over stdio.
One MCP session stays open for the whole review, so the server process (and its
backend login) is started once instead of once per tool call.

The agent only gets the two *read* tools it may choose between. `evaluate_rules`
is deliberately left out: rule evaluation is a fixed graph node, not something
the model decides to do (invariant I2).
"""

from __future__ import annotations

import sys
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

MCP_SERVER_NAME = "workflow"

# The agent's whole action surface. Anything not listed here is not offered to it.
AGENT_TOOL_NAMES = ("get_request_context", "search_policy")


def mcp_connections() -> dict:
    """How to start the MCP server: same interpreter, stdio transport."""
    return {
        MCP_SERVER_NAME: {
            "command": sys.executable,
            "args": ["-m", "agent.mcp_server"],
            "transport": "stdio",
        }
    }


@asynccontextmanager
async def open_agent_tools() -> AsyncIterator[list]:
    """Start the MCP server and yield the agent's tools for the duration of a review."""
    from langchain_mcp_adapters.client import MultiServerMCPClient
    from langchain_mcp_adapters.tools import load_mcp_tools

    client = MultiServerMCPClient(mcp_connections())
    async with client.session(MCP_SERVER_NAME) as session:
        tools = await load_mcp_tools(session)
        yield [tool for tool in tools if tool.name in AGENT_TOOL_NAMES]
