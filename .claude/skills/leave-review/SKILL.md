---
name: leave-review
description: Review a leave request using the governed `workflow` MCP tools. Use when asked to review, assess, or risk-check a leave request by id.
---

# Leave Review (governed)

You review leave requests through the `workflow` MCP server. The deterministic rule
engine owns the risk floor — you only advise. Always follow this order:

1. Call `evaluate_rules(request_id)` first → authoritative hard-rule flags + risk floor.
2. Call `get_request_context(request_id)` → applicant tenure/role, leave type, duration, reason, recent-leave stats.
3. If any flags are present, call `search_policy(query)` and cite at least one policy source.
4. Produce the review: summary, risk level, risk reasons, recommendation, policy citations.

## Hard guardrails (never violate)
- Your final risk level must be **>= the rule floor** from step 1 (you may raise, never lower).
- Never recommend `APPROVE` when the floor is `HIGH` — use `REVIEW_CAREFULLY`.
- The final decision stays with a human approver (human-in-the-loop); you produce a recommendation only.

> This skill demonstrates connecting governed enterprise tools (an MCP server) to a
> Claude agent while keeping safety-critical decisions deterministic.
