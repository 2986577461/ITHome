"""
RAGAS 评估 — 知识库 RAG 质量离线评估（CLI 模式）

使用方式：
  python3 -m eval.router
"""

import asyncio
import warnings
from datetime import datetime

from langchain.chat_models import init_chat_model
from dotenv import load_dotenv


def _safe_mean(series) -> float:
    val = series.mean()
    return float(val) if not (val != val) else 0.0


load_dotenv()


class RAGEvaluator:
    def __init__(self, llm=None):
        self.llm = llm
        self._ready = False
        self.last_sample = None

    def _import_ragas(self):
        from datasets import Dataset
        from ragas import evaluate
        from ragas.metrics import faithfulness, LLMContextPrecisionWithoutReference, ContextRelevance, ContextUtilization

        self.evaluate = evaluate
        self.faithfulness = faithfulness
        self.context_precision = LLMContextPrecisionWithoutReference()
        self.context_relevance = ContextRelevance()
        self.context_utilization = ContextUtilization()
        self.Dataset = Dataset

        if self.llm is None:
            self.llm = init_chat_model(model="deepseek-v4-flash", max_retries=0)

        warnings.filterwarnings("ignore", message="LangchainLLMWrapper is deprecated")
        from ragas.llms import LangchainLLMWrapper
        wrapper = LangchainLLMWrapper(self.llm)
        self.faithfulness.llm = wrapper
        self.context_precision.llm = wrapper
        self.context_relevance.llm = wrapper
        self.context_utilization.llm = wrapper
        self._ready = True

    def run(self, question: str, contexts: list[str], answer: str) -> dict:
        if not self._ready:
            self._import_ragas()
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
        """以最终回答实际使用的上下文评分，不重新检索，不混入中间答案。"""
        from eval.execution import collect_sample
        from kb.tools import search_knowledge_base
        from agent.tools import get_current_time

        if not isinstance(user_id, str) or not user_id.strip():
            raise ValueError("评估必须指定有效用户")
        self.last_sample = None
        if self.llm is None:
            self.llm = init_chat_model(model="deepseek-v4-flash", max_retries=0)
        self.last_sample = asyncio.run(collect_sample(
            question, user_id, self.llm, [get_current_time, search_knowledge_base],
        ))
        sample = self.last_sample
        print(sample.answer)
        if not sample.contexts:
            # 没调用 KB / 无相关片段 / 检索失败都不是可评分的 RAG 样本。
            status = "retrieval_failed" if any(r["status"] == "retrieval_failed" for r in sample.run.retrievals) else "no_context"
            return {"evaluation_status": status, "kb_tool_calls": len(sample.run.retrievals)}
        return self.run(question, sample.contexts, sample.answer)



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

    user_id = input("评估用户学号: ").strip()
    if not user_id:
        print("必须指定评估用户，不能进行全库检索")
        return

    print(f"\n正在评估...")
    t0 = datetime.now()

    evaluator = RAGEvaluator()
    metrics = evaluator.evaluate_question(question, user_id)

    elapsed = (datetime.now() - t0).total_seconds()
    _print_report(metrics, elapsed)


if __name__ == "__main__":
    main()