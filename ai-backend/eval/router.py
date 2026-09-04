"""
RAGAS 评估 — 知识库 RAG 质量离线评估（CLI 模式）

使用方式：
  python3 eval/router.py
"""

import os
import warnings
from datetime import datetime

from datasets import Dataset
from langchain.chat_models import init_chat_model
from langchain_core.messages import AIMessageChunk
from ragas import evaluate
from ragas.metrics import faithfulness, LLMContextPrecisionWithoutReference, ContextRelevance, ContextUtilization

from common.logger import get_agent_logger
from dotenv import load_dotenv
from common.vector_store import get_vector_store


def retrieve_chunks(query: str, user_id: str, top_k: int = 8) -> list[dict]:
    if not query.strip():
        return []
    try:
        hits = get_vector_store().search(query, user_id=user_id, top_k=top_k)
    except Exception:
        return []
    return [
        {"doc_id": h["doc_id"], "chunk_idx": h["chunk_idx"],
         "content": h["content"], "score": h["score"]}
        for h in hits
    ]


def _safe_mean(series) -> float:
    val = series.mean()
    return float(val) if not (val != val) else 0.0


load_dotenv()


class RAGEvaluator:
    def __init__(self, llm=None):
        self.llm = llm
        self._import_ragas()

    def _import_ragas(self):
        self.evaluate = evaluate
        self.faithfulness = faithfulness
        self.context_precision = LLMContextPrecisionWithoutReference()
        self.context_relevance = ContextRelevance()
        self.context_utilization = ContextUtilization()
        self.Dataset = Dataset

        if self.llm is None:
            self.llm = init_chat_model(model="deepseek-v4-flash")

        warnings.filterwarnings("ignore", message="LangchainLLMWrapper is deprecated")
        from ragas.llms import LangchainLLMWrapper
        wrapper = LangchainLLMWrapper(self.llm)
        self.faithfulness.llm = wrapper
        self.context_precision.llm = wrapper
        self.context_relevance.llm = wrapper
        self.context_utilization.llm = wrapper

    def run(self, question: str, contexts: list[str], answer: str) -> dict:
        dataset = self.Dataset.from_dict({
            "user_input": [question],
            "response": [answer],
            "retrieved_contexts": [contexts],
        })
        result = self.evaluate(
            dataset,
            metrics=[self.faithfulness, self.context_precision,
                     self.context_relevance, self.context_utilization],
        )
        df = result.to_pandas()
        return {
            "faithfulness": _safe_mean(df["faithfulness"]),
            "context_precision": _safe_mean(df["llm_context_precision_without_reference"]),
            "context_relevance": _safe_mean(df["nv_context_relevance"]),
            "context_utilization": _safe_mean(df["context_utilization"]),
        }

    def evaluate_question(self, question: str, user_id: str = "") -> dict:
        """单问题评估：agent 正常调用工具 → RAGAS 评分"""
        from langchain.agents import create_agent
        from langgraph.checkpoint.memory import InMemorySaver
        from kb.tools import search_knowledge_base
        from agent.tools import get_current_time

        sp_path = os.path.join(os.path.dirname(__file__), "..", "SystemPrompt.md")
        system_prompt = open(sp_path, encoding="utf-8").read().strip()

        checkpointer = InMemorySaver()
        agent = create_agent(
            model=self.llm,
            tools=[get_current_time, search_knowledge_base],
            checkpointer=checkpointer,
            system_prompt=system_prompt,
        )

        full_answer = ""
        alog = get_agent_logger()
        _thinking = []
        for chunk, metadata in agent.stream(
            {"messages": [{"role": "user", "content": question}]},
            config={"configurable": {"thread_id": f"eval:{user_id}:{question[:20]}",
                                     "user_id": user_id}},
            stream_mode="messages",
        ):
            if isinstance(chunk, AIMessageChunk) and chunk.content:
                full_answer += chunk.content
                _rc = chunk.additional_kwargs.get("reasoning_content", "")
                if _rc:
                    _thinking.append(_rc)
                elif _thinking:
                    _full = "".join(_thinking)
                    alog.info(0, "model_thinking", "模型推理", {"think": _full[:500]})
                    _thinking.clear()
        answer = full_answer.strip()
        print(answer)
        # 评估用的上下文直接从知识库检索（独立于 agent 是否调用了 KB）
        chunks = retrieve_chunks(question, user_id)
        contexts = [c["content"] for c in chunks]

        metrics = self.run(question, contexts, answer)
        return metrics



# ============================================================
# CLI 入口
# ============================================================

def _print_report(metrics: dict, elapsed: float):
    print()
    print("=" * 50)
    print("  RAGAS 评估报告")
    print("=" * 50)
    for name, val in metrics.items():
        label = f"{val:.3f}" if isinstance(val, (int, float)) and val == val else "N/A"
        print(f"  {name:<20} {label}")
    print(f"  {'耗时':<20} {elapsed:.1f}s")
    print("=" * 50)
    print()
    print("指标说明:")
    print("  faithfulness:       答案是否忠于检索到的上下文 (越高越好)")
    print("  context_precision:  检索结果中有多少是真正相关的 (越高越好)")
    print("  context_relevance:  检索到的上下文与问题的相关程度 (越高越好)")
    print("  context_utilization:回答在多大程度上利用了检索到的上下文 (越高越好)")
    print()


def main():
    print("RAGAS 评估 — 知识库 RAG 质量评估")
    print()

    question = input("问题: ").strip()
    if not question:
        print("问题不能为空")
        return

    user_id ="202300573"

    print(f"\n正在评估...")
    t0 = datetime.now()

    evaluator = RAGEvaluator()
    metrics = evaluator.evaluate_question(question, user_id)

    elapsed = (datetime.now() - t0).total_seconds()
    _print_report(metrics, elapsed)


if __name__ == "__main__":
    main()