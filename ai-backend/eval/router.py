"""
RAGAS 评估框架集成 — 对知识库 RAG 管道的检索与生成质量做量化评估

评估指标（无需 ground_truth，无需 embedding）：
  - faithfulness:                     答案是否忠于检索到的上下文（不编造）
  - llm_context_precision:            检索结果中有多少是真正相关的（LLM 评判版）
  - context_relevance:                检索到的上下文与问题的相关程度
  - context_utilization:              回答在多大程度上利用了检索到的上下文

使用方式：
  CLI: python3 ragas_evaluation.py                        # 交互式输入
  CLI: python3 ragas_evaluation.py --questions qs.json    # 从 JSON 文件读题
  API: 启动 Agent.py 后 POST /api/eval/run  {"questions":[...], "user_id":"..."}
"""

import json
import os
import sys
import sqlite3
import warnings
from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field

from kb.tools import get_kb_db
# 项目内部依赖
from common.vector_store import get_vector_store
from conversation.service import get_current_user

eval_router = APIRouter(prefix="/api/eval", tags=["RAGAS 评估"])







# ============================================================
# 一、纯检索函数（从 search_knowledge_base 提取，不含 LangChain 装饰器）
# ============================================================

def retrieve_chunks(query: str, user_id: str, top_k: int = 8) -> list[dict]:
    """
    对知识库执行语义向量检索，返回排序后的文档片段列表。
    底层使用 Milvus Lite + BGE 中文语义模型。
    """
    if not query.strip():
        return []
    try:
        hits = get_vector_store().search(query, user_id=user_id, top_k=top_k)
    except Exception as e:
        if "locked" in str(e).lower() or "lock" in str(e).lower():
            print("\n[错误] Milvus 数据库被占用，请先停止 Agent.py 再运行 CLI 评估")
            print("       或者改用 API: curl -X POST http://localhost:8000/api/eval/run ...\n")
        return []
    return [
        {"doc_id": h["doc_id"], "chunk_idx": h["chunk_idx"],
         "content": h["content"], "score": h["score"]}
        for h in hits
    ]


def _safe_mean(series) -> float:
    """取均值，全 NaN 时返回 0.0"""
    val = series.mean()
    return float(val) if not (val != val) else 0.0  # NaN != NaN


class RAGEvaluator:
    """封装 RAGAS 指标计算"""

    def __init__(self, llm=None):
        self.llm = llm
        self._import_ragas()

    def _import_ragas(self):
        """延迟导入 RAGAS，失败时给友好提示"""
        try:
            from langchain.chat_models import init_chat_model
            from ragas import evaluate
            from ragas.metrics import (
                faithfulness,
                LLMContextPrecisionWithoutReference,
                ContextRelevance,
                ContextUtilization,
            )
            from datasets import Dataset
        except ImportError as e:
            missing = str(e).split("'")[1] if "'" in str(e) else "ragas, datasets"
            print(f"[RAGAS] 缺少依赖: {missing}")
            print(f"[RAGAS] 请运行: pip3 install ragas datasets")
            sys.exit(1)

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

    def run(self, questions: list[str], contexts: list[list[str]],
            answers: list[str]) -> dict:
        """
        执行 RAGAS 评估。

        参数:
          questions: 问题列表
          contexts:  每个问题对应的检索上下文列表（每个元素是一个字符串列表）
          answers:   LLM 基于上下文生成的回答列表
        返回:
          {faithfulness, context_precision, context_relevance, context_utilization}
        """
        dataset = self.Dataset.from_dict({
            "user_input": questions,
            "response": answers,
            "retrieved_contexts": contexts,
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

    def run_with_llm(self, questions: list[str], user_id: str = "") -> dict:
        """
        端到端评估：检索 → Agent 生成 → RAGAS 评分。
        与线上 Agent.py 使用完全相同的 create_agent + SystemPrompt.md。
        """
        contexts = []
        answers = []

        for q in questions:
            chunks = retrieve_chunks(q, user_id)
            ctx_texts = [c["content"] for c in chunks]
            contexts.append(ctx_texts)

            # 注入上下文到 system prompt 中，绕过 KB 工具调用
            ctx_block = "\n\n---\n\n## 检索到的文档内容\n"
            ctx_block += "\n".join(f"- {t}" for t in ctx_texts) if ctx_texts else "(无相关文档)"
            sp_path = os.path.join(os.path.dirname(__file__), "SystemPrompt.md")
            enhanced_prompt = open(sp_path, encoding="utf-8").read().strip() + ctx_block

            from langchain.agents import create_agent
            from langgraph.checkpoint.memory import InMemorySaver
            
            
            @_tool
            def _eval_time() -> str:
                """获取当前时间"""
                return _dt.now().isoformat()

            checkpointer = InMemorySaver()
            agent = create_agent(
                model=self.llm,
                tools=[_eval_tool_time],
                checkpointer=checkpointer,
                system_prompt=enhanced_prompt,
            )

            full_answer = ""
            for chunk, metadata in agent.stream(
                {"messages": [{"role": "user", "content": q}]},
                config={"configurable": {"thread_id": f"eval:{user_id}:{q[:20]}"}},
                stream_mode="messages",
            ):
                if isinstance(chunk, AIMessageChunk) and chunk.content:
                    full_answer += chunk.content

            answers.append(full_answer.strip())
            print(f"  [Q] {q}")
            print(f"  [A] {full_answer[:200]}")

        metrics = self.run(questions, contexts, answers)
        metrics["commentary"] = self._generate_commentary(
            questions, answers, contexts, metrics
        )
        return metrics

    def _generate_commentary(self, questions, answers, contexts, metrics) -> str:
        """让 LLM 对评估结果写一段分析点评"""
        prompt = (
            "你是一个 RAG 系统评估分析师。根据以下数据写一段中文分析（100字以内），"
            "指出每个指标的得分说明了什么、高分好在哪里、低分差在哪里。\n\n"
            "评估指标：\n"
            f"  faithfulness（答案忠于上下文）: {metrics['faithfulness']:.2f}\n"
            f"  context_precision（检索结果相关性）: {metrics['context_precision']:.2f}\n"
            f"  context_relevance（上下文与问题相关性）: {metrics['context_relevance']:.2f}\n"
            f"  context_utilization（回答利用上下文程度）: {metrics['context_utilization']:.2f}\n\n"
            f"测试问题：\n"
        )
        q_list = "\n".join(f"- {q}" for q in questions)
        prompt += q_list + "\n\n分析："
        resp = self.llm.invoke(prompt)
        return resp.content if hasattr(resp, "content") else str(resp)



# ============================================================
# 三、请求/响应模型
# ============================================================

class EvalRequest(BaseModel):
    questions: list[str] = Field(..., description="待评估的问题列表（至少 3 个）")
    contexts: Optional[list[list[str]]] = Field(None, description="已检索到的上下文（可选，不传则自动检索）")
    answers: Optional[list[str]] = Field(None, description="LLM 已生成的回答（可选，不传则自动生成）")


class EvalResponse(BaseModel):
    ok: bool
    metrics: Optional[dict] = None
    error: Optional[str] = None


# ============================================================
# 四、API 路由
# ============================================================

@eval_router.post("/run", summary="运行 RAGAS 评估")
def api_run_eval(body: EvalRequest, user_id: str = Depends(get_current_user)):
    """
    运行 RAG 质量评估。
    如果提供 contexts 和 answers 则直接评分，否则自动检索+生成。
    """
    if len(body.questions) < 1:
        return EvalResponse(ok=False, error="至少需要 1 个问题")

    evaluator = RAGEvaluator()

    try:
        if body.contexts and body.answers:
            assert len(body.contexts) == len(body.questions), "contexts 数量必须与 questions 一致"
            assert len(body.answers) == len(body.questions), "answers 数量必须与 questions 一致"
            metrics = evaluator.run(body.questions, body.contexts, body.answers)
        else:
            metrics = evaluator.run_with_llm(body.questions, user_id)
    except Exception as e:
        return EvalResponse(ok=False, error=str(e))

    return EvalResponse(ok=True, metrics=metrics)


@eval_router.get("/health", summary="RAGAS 评估服务状态")
def eval_health():
    """检查 RAGAS 是否可用"""
    try:
        import ragas  # noqa: F401
        import datasets  # noqa: F401
        ragas_ok = True
    except ImportError:
        ragas_ok = False

    return {
        "ragas_available": ragas_ok,
        "doc_count": _count_docs(),
    }


def _count_docs() -> int:
    """统计知识库文档总数"""
    conn = get_kb_db()
    try:
        row = conn.execute("SELECT COUNT(*) FROM documents").fetchone()
        return row[0] if row else 0
    finally:
        conn.close()


# ============================================================
# 五、CLI 入口
# ============================================================

def _print_report(metrics: dict, elapsed: float):
    """打印评估报告"""
    print()
    print("=" * 50)
    print("  RAGAS 评估报告")
    print("=" * 50)
    for name, val in metrics.items():
        if name == "commentary":
            continue
        disp = val if isinstance(val, (int, float)) and val == val else 0.0
        stars = "\u2605" * int(disp * 5) + "\u2606" * (5 - int(disp * 5))
        label = f"{val:.3f}" if isinstance(val, (int, float)) and val == val else "N/A"
        print(f"  {name:<20} {label}  {stars}")
    print(f"  \u8017\u65f6:                {elapsed:.1f}s")
    print("=" * 50)
    print()
    commentary = metrics.get("commentary", "")
    if commentary:
        print("  LLM \u8bc4\u4f30\u5206\u6790:")
        for line in commentary.split("\n"):
            print(f"    {line}")
        print()
    print("\u6307\u6807\u8bf4\u660e:")
    print("  faithfulness:       \u7b54\u6848\u662f\u5426\u5fe0\u4e8e\u68c0\u7d22\u5230\u7684\u4e0a\u4e0b\u6587 (\u8d8a\u9ad8\u8d8a\u597d)")
    print("  context_precision:  \u68c0\u7d22\u7ed3\u679c\u4e2d\u6709\u591a\u5c11\u662f\u771f\u6b63\u76f8\u5173\u7684 (\u8d8a\u9ad8\u8d8a\u597d)")
    print("  context_relevance:  \u68c0\u7d22\u5230\u7684\u4e0a\u4e0b\u6587\u4e0e\u95ee\u9898\u7684\u76f8\u5173\u7a0b\u5ea6 (\u8d8a\u9ad8\u8d8a\u597d)")
    print("  context_utilization:\u56de\u7b54\u5728\u591a\u5927\u7a0b\u5ea6\u4e0a\u5229\u7528\u4e86\u68c0\u7d22\u5230\u7684\u4e0a\u4e0b\u6587 (\u8d8a\u9ad8\u8d8a\u597d)")
    print()


def _interactive():
    """交互式 CLI"""
    print("RAGAS 评估 — 知识库 RAG 质量评估")
    print("注意: 请先停止 Agent.py 再运行本命令，Milvus 不支持多进程访问")
    print()
    print("输入问题（每行一个，空行结束）：")
    questions = []
    while True:
        try:
            line = input("  > ").strip()
        except (EOFError, KeyboardInterrupt):
            break
        if not line and questions:
            break
        if line:
            questions.append(line)

    if len(questions) < 1:
        print("至少需要 1 个问题")
        return

    user_id = "202300573"

    print(f"\n正在对 {len(questions)} 个问题运行 RAGAS 评估...")
    t0 = datetime.now()

    evaluator = RAGEvaluator()
    metrics = evaluator.run_with_llm(questions, user_id)

    elapsed = (datetime.now() - t0).total_seconds()
    _print_report(metrics, elapsed)


def _from_json(path: str):
    """从 JSON 文件读取问题并评估"""
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    questions = data.get("questions", data if isinstance(data, list) else [])
    if not questions:
        print(f"JSON 文件 {path} 中未找到 questions 字段")
        sys.exit(1)

    user_id = data.get("user_id", "")

    print(f"从 {path} 加载了 {len(questions)} 个问题")
    t0 = datetime.now()

    evaluator = RAGEvaluator()
    metrics = evaluator.run_with_llm(questions, user_id)

    elapsed = (datetime.now() - t0).total_seconds()
    _print_report(metrics, elapsed)


if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[1] == "--questions":
        _from_json(sys.argv[2])
    else:
        _interactive()