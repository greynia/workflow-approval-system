"""Unit tests for the retrieval eval's scoring and its question file.

These need no backend and no API key, so they run in CI. They check that the scoring is right
and that the question file stays in step with the policy document; the retrieval quality itself
is measured by running ``retrieval_eval.py``.
"""

import pytest
import retrieval_eval as ev
import yaml

ANY = ev.Question(id="q1", type="t", query="?", expected=("A", "B"))
ALL = ev.Question(id="q2", type="t", query="?", expected=("A", "B"), match="all")
NONE = ev.Question(id="q3", type="no_answer", query="?", expect_no_match=True)


def test_any_match_uses_the_best_ranked_expected_section():
    ranked = ["X", "B", "A"]
    assert ev.rank_of_expected(ranked, ANY) == 2
    assert not ev.hit_at_k(ranked, ANY, 1)
    assert ev.hit_at_k(ranked, ANY, 3)
    assert ev.reciprocal_rank(ranked, ANY) == 0.5


def test_all_match_needs_every_section_and_ranks_by_the_last_one():
    assert ev.rank_of_expected(["A", "X", "B"], ALL) == 3
    assert ev.rank_of_expected(["A", "X", "Y"], ALL) is None
    assert ev.reciprocal_rank(["A", "X", "Y"], ALL) == 0.0


def test_a_miss_scores_zero():
    assert ev.rank_of_expected(["X", "Y"], ANY) is None
    assert not ev.hit_at_k(["X", "Y"], ANY, 3)
    assert ev.reciprocal_rank([], ANY) == 0.0


def test_dedupe_keeps_first_appearance():
    assert ev.dedupe(["A", "B", "A", "C", "B"]) == ["A", "B", "C"]


def test_summary_groups_by_type_and_totals_the_answerable_questions():
    other = ev.Question(id="q4", type="u", query="?", expected=("C",))
    run = ev.Run("r", 5, True, {"q1": ["A"], "q4": ["X", "C"], "q3": []})
    rows = {row["type"]: row for row in ev.summarize([ANY, other, NONE], run)}

    assert rows["t"]["hit@1"] == 1.0
    assert rows["u"]["hit@1"] == 0.0 and rows["u"]["hit@3"] == 1.0 and rows["u"]["mrr"] == 0.5
    assert rows["answerable"]["n"] == 2
    assert rows["answerable"]["hit@1"] == 0.5
    assert rows["answerable"]["mrr"] == pytest.approx(0.75)
    assert rows["no_answer"] == {"type": "no_answer", "n": 1, "correct": 1}


def test_no_answer_questions_are_only_scored_when_the_run_can_return_nothing():
    always_returns = ev.Run("r", 5, False, {"q3": ["A"]})
    filtering = ev.Run("r", 5, True, {"q3": ["A"]})

    assert ev.summarize([NONE], always_returns)[-1]["correct"] is None
    assert ev.misses([NONE], always_returns, 3) == []
    assert ev.summarize([NONE], filtering)[-1]["correct"] == 0
    assert ev.misses([NONE], filtering, 3) == [NONE]


def test_tokenize_emits_chinese_bigrams_and_ascii_words():
    assert ev.tokenize("病假 30 天") == ["30", "病假", "天"]
    assert ev.tokenize("代理人") == ["代理", "理人"]


def test_keyword_baseline_ranks_the_section_named_in_the_query_first():
    sections = ev.parse_policy_sections(ev.POLICY_PATH.read_text("utf-8"))
    assert ev.keyword_rank("職務代理人", sections, 5)[0] == "職務代理人"
    assert ev.keyword_rank("xyz", sections, 5) == []


def test_a_saved_run_reloads_and_writes_a_markdown_report(tmp_path):
    run = ev.Run("vector", 5, False, {"q1": ["A"], "q2": ["A"]}, avg_latency_ms=12.0)
    path = tmp_path / "run.json"

    ev.save_run(run, [ANY, ALL], path)

    assert ev.load_run(path) == run
    report = path.with_suffix(".md").read_text("utf-8")
    assert "| q1 | t | yes |" in report
    assert "| q2 | t | no |" in report


def test_question_file_matches_the_policy_document():
    data = yaml.safe_load(ev.QUESTIONS_PATH.read_text("utf-8"))
    headings = [name for name, _ in ev.parse_policy_sections(ev.POLICY_PATH.read_text("utf-8"))]
    questions = ev.load_questions()

    assert data["sections"] == headings
    assert len({q.id for q in questions}) == len(questions)
    for q in questions:
        assert bool(q.expected) != q.expect_no_match, q.id
        assert q.match in ("any", "all"), q.id
        assert set(q.expected) <= set(headings), q.id
    for row in data["questions"]:
        assert set(row.get("also_relevant", [])) <= set(headings), row["id"]
    assert {s for q in questions for s in q.expected} == set(headings)
