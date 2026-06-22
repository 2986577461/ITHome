"""Pydantic 请求/响应模型"""

from typing import Optional
from pydantic import BaseModel, Field


# -- KB --
class KbUploadRequest(BaseModel):
    filename: str = Field(..., description="文件名，如 doc.txt")
    content_b64: str = Field(..., description="文件内容的 base64 编码")


# -- Agent --
class ArticleInput(BaseModel):
    type: int = Field(...,
                      description="文章类型编号：1=c/c++, 2=前端, 3=数据结构与算法, 4=mysql数据库, 5=java, 6=python/AI")
    head: str = Field(..., description="文章标题，15字以内")
    content: str = Field(..., description="文章正文 HTML")


# -- Task --
class CreateTaskRequest(BaseModel):
    question: str = Field(..., description="用户问题")
    thread_id: str = Field(default="default", description="会话 ID")


# -- Eval --
class EvalRequest(BaseModel):
    questions: list[str] = Field(..., description="待评估的问题列表（至少 3 个）")
    contexts: Optional[list[list[str]]] = Field(None, description="已检索到的上下文（可选，不传则自动检索）")
    answers: Optional[list[str]] = Field(None, description="LLM 已生成的回答（可选，不传则自动生成）")


class EvalResponse(BaseModel):
    ok: bool
    metrics: Optional[dict] = None
    error: Optional[str] = None
