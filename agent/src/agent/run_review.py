"""Full agent review entrypoint.  [Phase 2+]

Usage:  python -m agent.run_review --request-id 1

Until the graph is wired (Phase 2), this prints guidance and points you at the
Phase 0 smoke test.
"""

from __future__ import annotations

import argparse


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the governed agent review")
    parser.add_argument("--request-id", type=int, required=True)
    parser.add_argument("--locale", default="en", choices=["en", "zh-TW"])
    args = parser.parse_args()

    try:
        from .graph import build_graph

        graph = build_graph()
    except NotImplementedError as exc:
        print("Agent graph not implemented yet (Phase 2).")
        print(f"  reason: {exc}")
        print("\nFor Phase 0 connectivity, run:")
        print("  python -m agent.smoke --request-id <id>")
        return

    result = graph.invoke({"request_id": args.request_id, "locale": args.locale})
    final = result["final"]
    print(final.model_dump_json(indent=2))


if __name__ == "__main__":
    main()
