"""使用实际 Agent 输入构造评估样本；不在回答后重新检索。"""

import asyncio
from dataclasses import dataclass

from langchain.agents import create_agent
from langchain_core.messages import AIMessage, ToolMessage

from agent.harness import HarnessError, HarnessMiddleware, Limits, OutputBudget, RunState
from agent.service import _text_content, load_system_prompt


@dataclass
class EvaluationSample:
    question: str
    answer: str
    contexts: list[str]
    run: RunState


def final_model_contexts(run: RunState) -> list[str]:
    """只评估最终回答那次模型输入里实际保留的 KB 片段。"""
    if not run.model_contexts:
        return []
    contexts = []
    for message in run.model_contexts[-1]["messages"]:
        if isinstance(message, ToolMessage) and isinstance(message.content, str) and message.content.startswith("[KB]"):
            parts = message.content.split("\n---\n", 1)
            # 无结果/失败的工具说明不当作检索证据。
            artifact = message.artifact
            if len(parts) == 2 and isinstance(artifact, dict) and artifact.get("status") == "success":
                expected = artifact.get("contexts", [])
                contexts.extend(expected if "\n\n".join(expected) == parts[1] else [parts[1]])
    return contexts


async def collect_sample(question: str, user_id: str, model, tools: list, *, limits: Limits | None = None) -> EvaluationSample:
    if not isinstance(user_id, str) or not user_id.strip():
        raise ValueError("评估必须指定有效用户，不允许全库检索")
    limits = limits or Limits.from_env()
    if not question.strip() or len(question) > limits.max_input_chars:
        raise ValueError("评估问题为空或超过长度限制")
    run = RunState(limits)
    agent = create_agent(model=model, tools=tools, system_prompt=load_system_prompt(),
                         middleware=[HarnessMiddleware(run)])
    try:
        async with asyncio.timeout(run.remaining()):
            result = await agent.ainvoke(
                {"messages": [{"role": "user", "content": question}]},
                config={"configurable": {"user_id": user_id},
                        "recursion_limit": 4 * (limits.max_model_calls + limits.max_tool_calls) + 10,
                        "callbacks": [OutputBudget(run)],
                        "metadata": {"run_id": run.run_id}},
            )
        final = result["messages"][-1]
        answer = _text_content(final.content) if isinstance(final, AIMessage) else ""
        if len(answer) > limits.max_output_chars:
            raise HarnessError("output_limit", "回答超过长度预算")
        return EvaluationSample(question, answer, final_model_contexts(run), run)
    finally:
        run.stopped = True
