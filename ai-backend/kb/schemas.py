"""知识库上传契约；限制单个文档，避免无界解码和向量化。"""

from pydantic import BaseModel, ConfigDict, Field, field_validator

MAX_DOCUMENT_BYTES = 1024 * 1024  # 1 MiB UTF-8 原文
MAX_BASE64_LENGTH = 4 * ((MAX_DOCUMENT_BYTES + 2) // 3)
CHUNK_OVERLAP = 60


class UploadDocumentRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    filename: str = Field(min_length=1, max_length=255)
    content_b64: str = Field(min_length=1, max_length=MAX_BASE64_LENGTH)

    @field_validator("filename")
    @classmethod
    def validate_filename(cls, value: str) -> str:
        value = value.strip()
        if not value or any(c in value for c in ("/", "\\")) or any(ord(c) < 32 for c in value):
            raise ValueError("文件名不能为空，或包含路径、控制字符")
        return value
