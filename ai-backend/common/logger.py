"""
agent_logger.py — Agent 结构化日志追踪

格式：每行一个 JSON 对象，兼容 ELK 消费。
字段：t(时间) lvl(级别) task(追踪id) step(步骤) state(状态) msg(描述) data(结构化数据)

用法：
  alog = get_agent_logger()
  alog.info(task_id, step, "searching_kb", "检索知识库", {"query": q})
  alog.error(task_id, step, "failed", "工具调用超时", {"tool": "web_search"})
"""

import json
import logging
import logging.handlers
import os
from datetime import datetime

LOG_DIR = os.path.join(os.path.dirname(__file__), "logs")
os.makedirs(LOG_DIR, exist_ok=True)

SENSITIVE_KEYS = {"token", "api_key", "authorization", "password", "secret", "cookie"}


def _redact(data: dict) -> dict:
    """递归脱敏敏感字段"""
    result = {}
    for k, v in data.items():
        if any(s in k.lower() for s in SENSITIVE_KEYS):
            result[k] = "****"
        elif isinstance(v, dict):
            result[k] = _redact(v)
        elif isinstance(v, str) and len(v) > 200:
            result[k] = v[:200] + "..."
        else:
            result[k] = v
    return result


class AgentLogger:
    def __init__(self, log_dir: str = LOG_DIR):
        self.logger = logging.getLogger("agent_trace")
        self.logger.setLevel(logging.INFO)

        # 避免重复添加 handler
        if self.logger.handlers:
            return

        # 每日滚动文件，保留 7 天
        handler = logging.handlers.TimedRotatingFileHandler(
            os.path.join(log_dir, "agent.log"),
            when="midnight",
            backupCount=7,
            encoding="utf-8",
        )
        handler.setFormatter(logging.Formatter("%(message)s"))
        self.logger.addHandler(handler)

        # 也输出到 stdout（本地开发方便看）
        console = logging.StreamHandler()
        console.setFormatter(logging.Formatter("%(message)s"))
        self.logger.addHandler(console)

    def _log(self, level: str, task_id: str, step: int, state: str, msg: str,
             data: dict | None = None, exc_info=None):
        entry = {
            "t": datetime.now().isoformat(),
            "lvl": level,
            "task": task_id[:16] if task_id else "-",
            "step": step,
            "state": state,
            "msg": msg,
        }
        if data:
            entry["data"] = _redact(data)
        self.logger.log(
            getattr(logging, level, logging.INFO),
            json.dumps(entry, ensure_ascii=False),
            exc_info=exc_info,
        )

    def info(self, task_id: str, step: int, state: str, msg: str,
             data: dict | None = None):
        self._log("INFO", task_id, step, state, msg, data)

    def warn(self, task_id: str, step: int, state: str, msg: str,
             data: dict | None = None):
        self._log("WARN", task_id, step, state, msg, data)

    def error(self, task_id: str, step: int, state: str, msg: str,
              data: dict | None = None, exc_info=None):
        self._log("ERROR", task_id, step, state, msg, data, exc_info)


_logger: AgentLogger | None = None


def get_agent_logger() -> AgentLogger:
    global _logger
    if _logger is None:
        _logger = AgentLogger()
    return _logger