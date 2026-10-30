"""
agent_logger.py — Agent 结构化日志追踪

格式：每行一个 JSON 对象，兼容 ELK 消费。
字段：t(时间) lvl(级别) run_id(运行标识，可选) step(步骤) state(状态) msg(描述) data(结构化数据)

用法：
  alog = get_agent_logger()
  alog.info(1, "searching_kb", "检索知识库", {"run_id": run_id, "hit_count": 3})
  alog.error(2, "failed", "工具调用超时", {"run_id": run_id, "tool": "web_search"})
"""

import json
import logging
import logging.handlers
import os
from datetime import datetime

LOG_DIR = os.path.join(os.path.dirname(__file__), "logs")
os.makedirs(LOG_DIR, exist_ok=True)

SENSITIVE_KEYS = {"token", "api_key", "authorization", "password", "secret", "cookie"}
USAGE_KEYS = {"input_tokens", "output_tokens", "total_tokens", "prompt_tokens", "completion_tokens",
              "input_token_details", "output_token_details", "completion_tokens_details", "prompt_tokens_details",
              "reasoning_tokens", "cached_tokens", "token_usage"}


def _redact(data):
    """递归脱敏字典和列表；Token 用量不是凭证，允许记录。"""
    if isinstance(data, dict):
        result = {}
        for key, value in data.items():
            name = str(key).lower()
            metric = name in USAGE_KEYS and isinstance(value, (int, dict))
            sensitive = any(marker in name for marker in SENSITIVE_KEYS) and not metric
            result[key] = "****" if sensitive else _redact(value)
        return result
    if isinstance(data, (list, tuple)):
        return [_redact(value) for value in data]
    if isinstance(data, str) and len(data) > 200:
        return data[:200] + "..."
    return data


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

    def _log(self, level: str, step: int, state: str, msg: str,
             data: dict | None = None, exc_info=None):
        entry = {
            "t": datetime.now().isoformat(),
            "lvl": level,
            "step": step,
            "state": state,
            "msg": msg,
        }
        if data:
            entry["data"] = _redact(data)
            if data.get("run_id"):
                entry["run_id"] = data["run_id"]
        self.logger.log(
            getattr(logging, level, logging.INFO),
            json.dumps(entry, ensure_ascii=False),
            exc_info=exc_info,
        )

    def info(self, step: int, state: str, msg: str,
             data: dict | None = None):
        self._log("INFO", step, state, msg, data)

    def warn(self, state: str, msg: str,
             data: dict | None = None):
        self._log("WARN",0, state, msg, data)

    def error(self, step: int, state: str, msg: str,
              data: dict | None = None, exc_info=None):
        self._log("ERROR", step, state, msg, data, exc_info)


_logger: AgentLogger | None = None


def get_agent_logger() -> AgentLogger:
    global _logger
    if _logger is None:
        _logger = AgentLogger()
    return _logger