"""Extract readable text and split it at paragraph boundaries for knowledge ingestion."""

import io
import re
import zipfile

from docx import Document
from docx.table import Table
from docx.text.paragraph import Paragraph
from pypdf import PdfReader

MAX_EXTRACTED_CHARS = 2_000_000
MAX_DOCX_UNCOMPRESSED = 20 * 1024 * 1024
MAX_PDF_PAGES = 100
CHUNK_SIZE = 500
OVERLAP = 60


class DocumentError(ValueError):
    pass


def _docx_text(raw: bytes) -> str:
    # DOCX is a ZIP archive. Check declared uncompressed sizes before parsing XML.
    try:
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            entries = archive.infolist()
            if len(entries) > 2000 or sum(x.file_size for x in entries) > MAX_DOCX_UNCOMPRESSED:
                raise DocumentError("Word 文件解压后过大")
        doc = Document(io.BytesIO(raw))
        blocks = []
        for block in doc.iter_inner_content():
            if isinstance(block, Paragraph):
                if block.text.strip():
                    blocks.append(block.text)
            elif isinstance(block, Table):
                for row in block.rows:
                    # Consecutive merged cells can refer to the same XML cell.
                    cells = []
                    previous = None
                    for cell in row.cells:
                        if cell._tc is previous:
                            continue
                        previous = cell._tc
                        cells.append(cell.text.replace("\n", " ").strip())
                    line = " | ".join(cells).strip(" |")
                    if line:
                        blocks.append(line)
        return "\n\n".join(blocks)
    except DocumentError:
        raise
    except Exception as exc:
        raise DocumentError("Word 文件无效或无法解析") from exc


def _pdf_text(raw: bytes) -> str:
    try:
        pdf = PdfReader(io.BytesIO(raw))
        if pdf.is_encrypted:
            raise DocumentError("不支持加密 PDF")
        if len(pdf.pages) > MAX_PDF_PAGES:
            raise DocumentError("PDF 不能超过 100 页")
        pages = []
        size = 0
        for page in pdf.pages:
            text = page.extract_text() or ""
            # Fail closed: a textless page could be a scan. Without OCR we cannot
            # distinguish it reliably from an intentionally blank page.
            if not text.strip():
                raise DocumentError("PDF 包含无法提取文字的页面（可能是扫描件或空白页），当前不支持 OCR，请处理后重新上传")
            size += len(text)
            if size > MAX_EXTRACTED_CHARS:
                raise DocumentError("提取的文本过大")
            pages.append(text)
        return "\n\n".join(pages)
    except DocumentError:
        raise
    except Exception as exc:
        raise DocumentError("PDF 文件无效或无法解析") from exc


def extract_text(raw: bytes, ext: str) -> str:
    if ext in ("txt", "md"):
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise DocumentError("文档必须使用 UTF-8 编码") from exc
    elif ext == "docx":
        text = _docx_text(raw)
    elif ext == "pdf":
        text = _pdf_text(raw)
    else:
        raise DocumentError("仅支持 txt、md、docx 和 pdf")
    if len(text) > MAX_EXTRACTED_CHARS:
        raise DocumentError("提取的文本过大")
    if not text.strip():
        raise DocumentError("未提取到文本；扫描版 PDF 需要 OCR，当前不支持")
    return text


def _paragraphs(text: str, markdown: bool) -> list[str]:
    # Blank lines delimit paragraphs. Markdown fenced blocks stay together where possible.
    if not markdown:
        return [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]
    blocks, current = [], []
    fence = None
    for line in text.splitlines():
        match = re.match(r"^\s*(`{3,}|~{3,})", line)
        if fence:
            current.append(line)
            if match and match.group(1)[0] == fence[0] and len(match.group(1)) >= len(fence):
                fence = None
            continue
        if match:
            if current:
                blocks.append("\n".join(current))
                current = []
            fence = match.group(1)
            current.append(line)
        elif not line.strip():
            if current:
                blocks.append("\n".join(current))
                current = []
        else:
            current.append(line)
    if current:
        blocks.append("\n".join(current))
    return blocks


def chunk_text(text: str, ext: str, size: int = CHUNK_SIZE, overlap: int = OVERLAP) -> list[str]:
    if size <= 0 or not 0 <= overlap < size:
        raise ValueError("invalid chunk size or overlap")
    chunks = []
    current = ""
    for paragraph in _paragraphs(text, ext == "md"):
        # Long paragraphs are split near a sentence boundary when possible.
        while len(paragraph) > size:
            if current:
                chunks.append(current)
                current = ""
            cut = max((paragraph.rfind(c, size // 2, size) + 1 for c in "。！？!?；;\n"), default=0)
            if cut <= 0:
                cut = size
            chunks.append(paragraph[:cut])
            paragraph = paragraph[max(1, cut - overlap):]
        if not paragraph:
            continue
        candidate = current + "\n\n" + paragraph if current else paragraph
        if len(candidate) <= size:
            current = candidate
        else:
            chunks.append(current)
            # Repeat only a complete paragraph when it fits the overlap budget.
            tail = current.split("\n\n")[-1]
            current = tail + "\n\n" + paragraph if len(tail) <= overlap and len(tail) + 2 + len(paragraph) <= size else paragraph
    if current:
        chunks.append(current)
    return chunks
