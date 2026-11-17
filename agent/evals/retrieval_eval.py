"""Retrieval-layer eval for the leave-policy RAG.

Answers one question: for each query in ``retrieval_questions.zh.yaml``, did the right policy
section come back, and how high was it ranked? It does not judge any LLM answer.

Two ways to get a ranking:

* ``--mode keyword``  Offline BM25 over the policy document. Needs no backend and no API key.
                      This is a baseline to compare against, not the system under test.
* ``--mode backend``  Calls the real ``/api/ai/policy-search`` endpoint (pgvector, plus the Jina
                      reranker when the backend has a rerank key). Needs a running backend that
                      was started with a Gemini key, and AGENT_PASSWORD in ``agent/.env``.

Usage (from the ``agent`` directory)::

    uv run python evals/retrieval_eval.py --mode keyword --save evals/results/keyword.json
    uv run python evals/retrieval_eval.py --mode backend --save evals/results/vector.json
    uv run python evals/retrieval_eval.py --compare evals/results/keyword.json \
        evals/results/vector.json

Metrics, reported per question type:

* Hit@k  share of questions whose expected section is within the first k results.
* MRR    mean of 1/rank of the expected section (rank 1 -> 1.0, rank 2 -> 0.5, missing -> 0).
* No-answer questions are correct only when nothing is returned, and are scored only for runs
  that drop low-score matches (the reranked backend path).

For ``match: all`` questions every expected section must appear; the rank used is the position
at which the last of them shows up.
"""

from __future__ import annotations

import argparse
import json
import math
import re
import sys
import time
from collections import Counter
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

import yaml

EVALS_DIR = Path(__file__).resolve().parent
QUESTIONS_PATH = EVALS_DIR / "retrieval_questions.zh.yaml"
POLICY_PATH = EVALS_DIR.parents[1] / "backend/src/main/resources/policy/leave-policy.zh.md"

KS = (1, 3)
DEFAULT_TOP_K = 5  # the backend's app.ai.policy.final-top-k
NO_ANSWER = "no_answer"
ANSWERABLE = "answerable"


# --- questions -----------------------------------------------------------------------------


@dataclass(frozen=True)
class Question:
    id: str
    type: str
    query: str
    expected: tuple[str, ...] = ()
    match: str = "any"  # any | all
    expect_no_match: bool = False


def load_questions(path: Path = QUESTIONS_PATH) -> list[Question]:
    data = yaml.safe_load(path.read_text("utf-8"))
    return [
        Question(
            id=row["id"],
            type=row["type"],
            query=row["query"],
            expected=tuple(row.get("expected", ())),
            match=row.get("match", "any"),
            expect_no_match=bool(row.get("expect_no_match", False)),
        )
        for row in data["questions"]
    ]


# --- metrics -------------------------------------------------------------------------------


def dedupe(sections: list[str]) -> list[str]:
    """A long section can be split into several chunks; rank by first appearance."""
    return list(dict.fromkeys(sections))


def rank_of_expected(ranked: list[str], question: Question) -> int | None:
    """1-based rank at which the question counts as answered, or None when it never does."""
    positions = [ranked.index(s) + 1 for s in question.expected if s in ranked]
    if not positions:
        return None
    if question.match == "all":
        return max(positions) if len(positions) == len(question.expected) else None
    return min(positions)


def hit_at_k(ranked: list[str], question: Question, k: int) -> bool:
    rank = rank_of_expected(ranked, question)
    return rank is not None and rank <= k


def reciprocal_rank(ranked: list[str], question: Question) -> float:
    rank = rank_of_expected(ranked, question)
    return 1.0 / rank if rank else 0.0


@dataclass
class Run:
    label: str
    top_k: int
    drops_low_scores: bool  # True when irrelevant queries can legitimately return nothing
    ranked: dict[str, list[str]]  # question id -> sections, best first
    avg_latency_ms: float | None = None  # per query; only measured against the real backend


def summarize(questions: list[Question], run: Run) -> list[dict]:
    """One row per answerable question type, an ``answerable`` total, then ``no_answer``."""
    answerable = [q for q in questions if not q.expect_no_match]
    groups: dict[str, list[Question]] = {}
    for q in answerable:
        groups.setdefault(q.type, []).append(q)
    groups[ANSWERABLE] = answerable

    rows = []
    for name, group in groups.items():
        row: dict = {"type": name, "n": len(group)}
        for k in KS:
            hits = sum(hit_at_k(run.ranked.get(q.id, []), q, k) for q in group)
            row[f"hit@{k}"] = hits / len(group) if group else 0.0
        row["mrr"] = (
            sum(reciprocal_rank(run.ranked.get(q.id, []), q) for q in group) / len(group)
            if group
            else 0.0
        )
        rows.append(row)

    no_answer = [q for q in questions if q.expect_no_match]
    if no_answer:
        correct = sum(not run.ranked.get(q.id) for q in no_answer) if run.drops_low_scores else None
        rows.append({"type": NO_ANSWER, "n": len(no_answer), "correct": correct})
    return rows


def misses(questions: list[Question], run: Run, k: int) -> list[Question]:
    out = []
    for q in questions:
        ranked = run.ranked.get(q.id, [])
        if q.expect_no_match:
            if run.drops_low_scores and ranked:
                out.append(q)
        elif not hit_at_k(ranked, q, k):
            out.append(q)
    return out


# --- keyword baseline (no backend, no key) -------------------------------------------------

_ASCII_TOKEN = re.compile(r"[a-z0-9]+")
_CJK_RUN = re.compile(r"[一-鿿]+")


def tokenize(text: str) -> list[str]:
    """ASCII words plus overlapping character bigrams for Chinese (no word segmenter needed)."""
    text = text.lower()
    tokens = _ASCII_TOKEN.findall(text)
    for run in _CJK_RUN.findall(text):
        tokens.extend([run] if len(run) == 1 else [run[i : i + 2] for i in range(len(run) - 1)])
    return tokens


def parse_policy_sections(markdown: str) -> list[tuple[str, str]]:
    """Split on ``## `` headings, the same unit the backend ingests as one chunk."""
    sections: list[tuple[str, str]] = []
    name: str | None = None
    body: list[str] = []
    for line in markdown.splitlines():
        if line.startswith("## "):
            if name is not None:
                sections.append((name, "\n".join(body).strip()))
            name, body = line[3:].strip(), []
        elif name is not None:
            body.append(line)
    if name is not None:
        sections.append((name, "\n".join(body).strip()))
    return sections


def keyword_rank(query: str, sections: list[tuple[str, str]], top_k: int) -> list[str]:
    """BM25 over ``heading + body`` (the same text the backend embeds). Zero scores are dropped."""
    k1, b = 1.5, 0.75
    docs = [Counter(tokenize(f"{name}\n{body}")) for name, body in sections]
    lengths = [sum(doc.values()) for doc in docs]
    avg_len = sum(lengths) / len(lengths)
    doc_freq = Counter(token for doc in docs for token in doc)

    scores = []
    for (name, _), doc, length in zip(sections, docs, lengths):
        score = 0.0
        for token in set(tokenize(query)):
            tf = doc.get(token, 0)
            if tf == 0:
                continue
            idf = math.log(1 + (len(docs) - doc_freq[token] + 0.5) / (doc_freq[token] + 0.5))
            score += idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * length / avg_len))
        scores.append((score, name))
    ranked = sorted((s for s in scores if s[0] > 0), key=lambda s: -s[0])
    return [name for _, name in ranked[:top_k]]


def keyword_run(questions: list[Question], top_k: int) -> Run:
    sections = parse_policy_sections(POLICY_PATH.read_text("utf-8"))
    ranked = {q.id: keyword_rank(q.query, sections, top_k) for q in questions}
    return Run("keyword-baseline", top_k, drops_low_scores=False, ranked=ranked)


# --- real backend --------------------------------------------------------------------------


def backend_run(questions: list[Question], top_k: int) -> Run:
    import httpx

    from agent.backend_client import BackendClient  # lazy: keyword mode needs no settings

    ranked: dict[str, list[str]] = {}
    reranked_flags: list[bool] = []
    started = time.perf_counter()
    try:
        with BackendClient() as client:
            client.login()  # keep the login round-trip out of the per-query latency
            started = time.perf_counter()
            for q in questions:
                resp = client.search_policy(q.query, top_k=top_k)
                ranked[q.id] = dedupe([m["section"] for m in resp["matches"]])
                reranked_flags.append(bool(resp["reranked"]))
    except httpx.ConnectError as e:
        raise SystemExit(f"Backend is not reachable ({e}). Start it first.") from e
    except httpx.HTTPStatusError as e:
        status = e.response.status_code
        if status == 404:
            raise SystemExit(
                "/api/ai/policy-search does not exist: the backend only registers it when it is "
                "started with a Gemini key (APP_AI_GEMINI_API_KEY)."
            ) from e
        if status in (401, 403):
            raise SystemExit("Login was rejected: check AGENT_PASSWORD in agent/.env.") from e
        raise

    reranked = all(reranked_flags)
    latency_ms = (time.perf_counter() - started) * 1000 / len(questions)
    return Run("vector+rerank" if reranked else "vector", top_k, reranked, ranked, latency_ms)


# --- reporting -----------------------------------------------------------------------------


def format_report(questions: list[Question], run: Run) -> str:
    lines = [
        f"Run: {run.label}  ({len(questions)} questions, top_k={run.top_k})",
        "",
        f"{'type':<16}{'n':>3}" + "".join(f"{f'Hit@{k}':>8}" for k in KS) + f"{'MRR':>8}",
    ]
    for row in summarize(questions, run):
        if row["type"] == NO_ANSWER:
            result = (
                "not scored (this run never returns an empty result)"
                if row["correct"] is None
                else f"{row['correct']}/{row['n']} returned nothing, as expected"
            )
            lines.append(f"{NO_ANSWER:<16}{row['n']:>3}  {result}")
            continue
        cells = "".join(f"{row[f'hit@{k}']:>8.2f}" for k in KS) + f"{row['mrr']:>8.2f}"
        lines.append(f"{row['type']:<16}{row['n']:>3}{cells}")

    if run.avg_latency_ms is not None:
        lines += ["", f"Average latency per query: {run.avg_latency_ms:.0f} ms"]

    k = KS[-1]
    missed = misses(questions, run, k)
    lines += ["", f"Misses at k={k}: {len(missed)}"]
    for q in missed:
        lines.append(f"  {q.id} [{q.type}] {q.query}")
        lines.append(f"      expected: {_expected_text(q)}")
        lines.append(f"      got:      {_got_text(run, q, k)}")
    return "\n".join(lines)


def _expected_text(q: Question) -> str:
    if q.expect_no_match:
        return "(nothing)"
    return (" + " if q.match == "all" else " / ").join(q.expected)


def _got_text(run: Run, q: Question, k: int) -> str:
    return ", ".join(run.ranked.get(q.id, [])[:k]) or "(nothing)"


def format_markdown(questions: list[Question], run: Run) -> str:
    """The same result as a Markdown report with one row per question, for the repo and README."""
    k = KS[-1]
    latency = "n/a" if run.avg_latency_ms is None else f"{run.avg_latency_ms:.0f} ms"
    lines = [
        f"# Retrieval eval: {run.label}",
        "",
        "| item | value |",
        "|---|---|",
        f"| created_at | {datetime.now():%Y-%m-%d %H:%M} |",
        f"| questions | {QUESTIONS_PATH.name} ({len(questions)}) |",
        f"| top_k | {run.top_k} |",
        f"| avg latency per query | {latency} |",
        "",
        "## Summary",
        "",
        "| type | n | " + " | ".join(f"Hit@{x}" for x in KS) + " | MRR |",
        "|---|---:|" + "---:|" * (len(KS) + 1),
    ]
    for row in summarize(questions, run):
        if row["type"] == NO_ANSWER:
            cell = "not scored" if row["correct"] is None else f"{row['correct']}/{row['n']}"
            lines += ["", f"No-answer questions that correctly returned nothing: {cell}"]
            continue
        cells = " | ".join(f"{row[f'hit@{x}']:.2f}" for x in KS)
        lines.append(f"| {row['type']} | {row['n']} | {cells} | {row['mrr']:.2f} |")

    missed = {q.id for q in misses(questions, run, k)}
    lines += [
        "",
        "## Cases",
        "",
        f"| id | type | hit@{k} | query | expected | got (top {k}) |",
        "|---|---|---|---|---|---|",
    ]
    for q in questions:
        scored = run.drops_low_scores or not q.expect_no_match
        mark = "-" if not scored else ("no" if q.id in missed else "yes")
        lines.append(
            f"| {q.id} | {q.type} | {mark} | {q.query} | {_expected_text(q)} "
            f"| {_got_text(run, q, k)} |"
        )
    return "\n".join(lines) + "\n"


def format_comparison(questions: list[Question], runs: list[Run]) -> str:
    metrics = " / ".join([f"Hit@{k}" for k in KS] + ["MRR"])
    width = max(18, *(len(r.label) + 2 for r in runs))
    lines = [
        f"Each cell: {metrics}",
        "",
        f"{'type':<16}{'n':>3}  " + "".join(f"{r.label:<{width}}" for r in runs),
    ]
    summaries = [{row["type"]: row for row in summarize(questions, r)} for r in runs]
    for name, first in summaries[0].items():
        cells = []
        for summary in summaries:
            row = summary[name]
            if name == NO_ANSWER:
                cell = "not scored" if row["correct"] is None else f"{row['correct']}/{row['n']}"
            else:
                cell = " / ".join([f"{row[f'hit@{k}']:.2f}" for k in KS] + [f"{row['mrr']:.2f}"])
            cells.append(f"{cell:<{width}}")
        lines.append(f"{name:<16}{first['n']:>3}  " + "".join(cells))
    return "\n".join(lines)


def save_run(run: Run, questions: list[Question], path: Path) -> None:
    """Writes ``path`` (JSON, reloadable by ``--compare``) and a Markdown report beside it."""
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "label": run.label,
        "top_k": run.top_k,
        "drops_low_scores": run.drops_low_scores,
        "avg_latency_ms": run.avg_latency_ms,
        "summary": summarize(questions, run),
        "ranked": run.ranked,
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", "utf-8")
    path.with_suffix(".md").write_text(format_markdown(questions, run), "utf-8")


def load_run(path: Path) -> Run:
    data = json.loads(path.read_text("utf-8"))
    return Run(
        data["label"],
        data["top_k"],
        data["drops_low_scores"],
        data["ranked"],
        data.get("avg_latency_ms"),
    )


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--mode", choices=["keyword", "backend"], default="keyword")
    parser.add_argument("--top-k", type=int, default=DEFAULT_TOP_K)
    parser.add_argument("--save", type=Path, help="write this run's rankings and summary as JSON")
    parser.add_argument(
        "--compare", type=Path, nargs="+", metavar="RUN.json", help="compare saved runs"
    )
    args = parser.parse_args(argv)
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")  # the report contains Chinese

    questions = load_questions()
    if args.compare:
        print(format_comparison(questions, [load_run(p) for p in args.compare]))
        return

    run = keyword_run if args.mode == "keyword" else backend_run
    result = run(questions, args.top_k)
    print(format_report(questions, result))
    if args.save:
        save_run(result, questions, args.save)
        print(f"\nSaved to {args.save} and {args.save.with_suffix('.md')}")


if __name__ == "__main__":
    main()
