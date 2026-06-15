"""LangGraph nodes.

Deterministic backbone (always runs, agent cannot bypass):
  evaluate_rules  -> authoritative flags + risk floor   (before the agent)
  enforce_floor   -> max(agent, floor) + downgrade rules (after the agent)

Agent-driven (soft reasoning only):
  agent_node      -> Gemini ReAct loop over MCP tools, produces a DRAFT
  submit          -> emit / persist the final review
"""
