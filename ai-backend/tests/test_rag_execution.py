"""评估只使用真实工具调用与最终模型输入，不二次检索。"""

import asyncio
from unittest.mock import Mock

from langchain_core.messages import AIMessageChunk
import pytest

from eval.execution import collect_sample
from test_agent_harness import ScriptedModel


@pytest.fixture(autouse=True)
def mock_committed_hit_validation(modules, monkeypatch):
    # These tests exercise evaluation with a synthetic vector store, not SQLite.
    monkeypatch.setattr(modules.tools, "_committed_hits", lambda hits: hits)


def test_sample_uses_rewritten_query_and_actual_context(modules, monkeypatch):
    store = Mock()
    store.search.return_value = [{"doc_id": 8, "chunk_idx": 2, "score": 0.9, "content": "actual evidence"}]
    monkeypatch.setattr(modules.tools, "get_vector_store", lambda: store)
    model = ScriptedModel(scripts=[
        [AIMessageChunk(content="intermediate thinking", tool_calls=[
            {"name": "search_knowledge_base", "args": {"query": "rewritten keywords"}, "id": "kb-call"},
        ])],
        [AIMessageChunk(content="final answer")],
    ])
    sample = asyncio.run(collect_sample("original question", "alice", model, [modules.tools.search_knowledge_base]))
    assert sample.answer == "final answer"
    assert sample.contexts == ["[文档#8 片段#2]\nactual evidence"]
    assert sample.run.retrievals[0]["query"] == "rewritten keywords"
    store.search.assert_called_once_with("rewritten keywords", user_id="alice", top_k=8)


def test_no_tool_call_has_no_synthetic_context(modules, monkeypatch):
    store = Mock()
    monkeypatch.setattr(modules.tools, "get_vector_store", lambda: store)
    sample = asyncio.run(collect_sample("q", "alice", ScriptedModel(scripts=[[AIMessageChunk(content="answer")]]),
                                        [modules.tools.search_knowledge_base]))
    assert sample.contexts == []
    store.search.assert_not_called()


def test_empty_identity_rejected_before_model_or_storage(modules):
    model = ScriptedModel(scripts=[])
    with pytest.raises(ValueError):
        asyncio.run(collect_sample("q", "", model, [modules.tools.search_knowledge_base]))
    assert model.calls == 0


@pytest.mark.parametrize("status", ["no_hits", "retrieval_failed", "no_relevant_context"])
def test_no_evidence_statuses_are_not_scoring_context(modules, monkeypatch, status):
    store = Mock()
    if status == "retrieval_failed":
        store.search.side_effect = RuntimeError("private details")
    else:
        store.search.return_value = [] if status == "no_hits" else [
            {"doc_id": 1, "chunk_idx": 0, "score": 0.1, "content": "irrelevant"},
        ]
    monkeypatch.setattr(modules.tools, "get_vector_store", lambda: store)
    model = ScriptedModel(scripts=[
        [AIMessageChunk(content="", tool_calls=[{"name": "search_knowledge_base", "args": {"query": "q"}, "id": "kb"}])],
        [AIMessageChunk(content="no evidence")],
    ])
    sample = asyncio.run(collect_sample("q", "alice", model, [modules.tools.search_knowledge_base]))
    assert sample.contexts == []
    assert sample.run.retrievals[0]["status"] == status


def test_no_tool_evaluation_does_not_load_or_call_ragas(modules, monkeypatch):
    from eval.router import RAGEvaluator
    model = ScriptedModel(scripts=[[AIMessageChunk(content="plain answer")]])
    evaluator = RAGEvaluator(llm=model)
    scorer = Mock(side_effect=AssertionError("无证据时不能调用 RAGAS"))
    monkeypatch.setattr(evaluator, "run", scorer)
    result = evaluator.evaluate_question("q", "alice")
    assert result == {"evaluation_status": "no_context", "kb_tool_calls": 0}
    scorer.assert_not_called()


def test_evaluator_passes_only_actual_context_to_scorer(modules, monkeypatch):
    from eval.router import RAGEvaluator
    store = Mock()
    store.search.return_value = [{"doc_id": 2, "chunk_idx": 5, "score": 0.9, "content": "exact evidence"}]
    monkeypatch.setattr(modules.tools, "get_vector_store", lambda: store)
    model = ScriptedModel(scripts=[
        [AIMessageChunk(content="", tool_calls=[{"name": "search_knowledge_base", "args": {"query": "rewrite"}, "id": "kb"}])],
        [AIMessageChunk(content="final")],
    ])
    evaluator = RAGEvaluator(llm=model)
    scorer = Mock(return_value={"faithfulness": 1})
    monkeypatch.setattr(evaluator, "run", scorer)
    assert evaluator.evaluate_question("original", "alice") == {"faithfulness": 1}
    scorer.assert_called_once_with("original", ["[文档#2 片段#5]\nexact evidence"], "final")
    assert store.search.call_count == 1


def test_retrieval_failure_is_distinct_from_absence_in_evaluation(modules, monkeypatch):
    from eval.router import RAGEvaluator
    store = Mock()
    store.search.side_effect = RuntimeError("private details")
    monkeypatch.setattr(modules.tools, "get_vector_store", lambda: store)
    model = ScriptedModel(scripts=[
        [AIMessageChunk(content="", tool_calls=[{"name": "search_knowledge_base", "args": {"query": "q"}, "id": "kb"}])],
        [AIMessageChunk(content="service unavailable")],
    ])
    result = RAGEvaluator(llm=model).evaluate_question("q", "alice")
    assert result["evaluation_status"] == "retrieval_failed"
